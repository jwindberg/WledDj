package com.example.wleddj.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.example.wleddj.data.model.AnimationRegion
import com.example.wleddj.data.model.Installation
import com.example.wleddj.data.model.WledDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.roundToInt

class RenderEngine(
    private var installation: Installation
) {


    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    
    // Render Bounds (World Space) - Auto-expanded to fit devices
    var renderBounds = RectF(0f, 0f, 1000f, 1000f)
        private set
    var renderOriginX = 0f
        private set
    var renderOriginY = 0f
        private set
    
    // Virtual buffer
    private var bufferBitmap: Bitmap = Bitmap.createBitmap(
        installation.width.toInt().coerceAtLeast(1),
        installation.height.toInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    private var canvas = Canvas(bufferBitmap)
    
    // Use Data Class to bundle Bitmap + Origin
    data class PreviewFrame(val bitmap: Bitmap, val originX: Float, val originY: Float)
    
    // Preview flow for UI
    private val _previewFrame = MutableStateFlow<PreviewFrame?>(null)
    val previewFrame = _previewFrame.asStateFlow()

    init {
        recalculateBounds()
    }

    // Animations (Regions)
    private val activeRegions = mutableListOf<AnimationRegion>()

    fun addRegion(region: AnimationRegion) {
        synchronized(activeRegions) {
            activeRegions.add(region)
        }
    }

    fun clearAnimations() {
        synchronized(activeRegions) {
            activeRegions.clear()
        }
    }
    
    fun getRegions(): List<AnimationRegion> {
        return synchronized(activeRegions) { activeRegions.toList() }
    }

    fun updateRegion(id: String, newRect: RectF, newRotation: Float) {
        synchronized(activeRegions) {
            val index = activeRegions.indexOfFirst { it.id == id }
            if (index != -1) {
                activeRegions[index] = activeRegions[index].copy(rect = newRect, rotation = newRotation)
            }
        }
    }
    
    fun removeRegion(id: String) {
        synchronized(activeRegions) {
            activeRegions.removeAll { it.id == id }
        }
    }

    // Network
    private val socket = DatagramSocket()

    fun updateInstallation(newInstallation: Installation) {
        installation = newInstallation
        recalculateBounds()
    }
    
    private fun recalculateBounds() {
        // Union of Canvas (0,0 -> W,H) and all Devices
        var minX = 0f
        var minY = 0f
        var maxX = installation.width
        var maxY = installation.height
        
        installation.devices.forEach { device ->
            // Calculate 4 corners of the rotated device
            val cx = device.x + device.width / 2f
            val cy = device.y + device.height / 2f
            val rad = Math.toRadians(device.rotation.toDouble())
            val cos = Math.cos(rad)
            val sin = Math.sin(rad)
            
            val w2 = device.width / 2f
            val h2 = device.height / 2f
            
            // Corners relative to center
            val corners = listOf(
                Pair(-w2, -h2), Pair(w2, -h2),
                Pair(w2, h2), Pair(-w2, h2)
            )
            
            corners.forEach { (dx, dy) ->
                val rx = (dx * cos - dy * sin) + cx
                val ry = (dx * sin + dy * cos) + cy
                
                if (rx < minX) minX = rx.toFloat()
                if (ry < minY) minY = ry.toFloat()
                if (rx > maxX) maxX = rx.toFloat()
                if (ry > maxY) maxY = ry.toFloat()
            }
        }
        
        // Add padding just in case
        val padding = 50f
        minX -= padding
        minY -= padding
        maxX += padding
        maxY += padding

        renderBounds.set(minX, minY, maxX, maxY)
        renderOriginX = minX
        renderOriginY = minY
        
        val newW = (maxX - minX).toInt().coerceAtLeast(1)
        val newH = (maxY - minY).toInt().coerceAtLeast(1)
        
        // Resize buffer if bounds size changed significantly or if previously default
        if (newW != bufferBitmap.width || newH != bufferBitmap.height) {
            bufferBitmap = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bufferBitmap)
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            while (isRunning && isActive) {
                val startTime = System.currentTimeMillis()
                
                renderFrame()
                mapAndSend()
                
                _previewFrame.emit(PreviewFrame(
                    bufferBitmap.copy(Bitmap.Config.ARGB_8888, false),
                    renderOriginX,
                    renderOriginY
                ))

                // Target 30 FPS (~33ms)
                val elapsed = System.currentTimeMillis() - startTime
                val wait = 33 - elapsed
                if (wait > 0) delay(wait)
            }
        }
    }

    fun stop() {
        isRunning = false
    }

    private fun renderFrame() {
        canvas.drawColor(Color.BLACK)
        
        // Global Transform: Translate World -> Bitmap Space
        // World (e.g. -500, -500) -> Bitmap (0, 0)
        canvas.save()
        canvas.translate(-renderOriginX, -renderOriginY)
        
        // Draw regions
        val regionsCopy = synchronized(activeRegions) { activeRegions.toList() }
        regionsCopy.forEach { region ->
            canvas.save()
            // 1. Rotate around region center
            canvas.rotate(region.rotation, region.rect.centerX(), region.rect.centerY())
            // 2. Clip
            canvas.clipRect(region.rect)
            // 3. Draw at Top-Left
            canvas.translate(region.rect.left, region.rect.top)
            region.animation.draw(canvas, region.rect.width(), region.rect.height())
            canvas.restore()
        }
        canvas.restore() 
    }

    private val deviceBuffers = mutableMapOf<String, ByteArray>()

    private suspend fun mapAndSend() {
        val install = installation // Capture reference
        install.devices.forEach { device ->
            try {
                // Reuse buffer or create new if size changed
                val bufferSize = device.pixelCount * 3
                val existing = deviceBuffers[device.macAddress] // specific unique ID? using MAC or IP as key
                val data = if (existing != null && existing.size == bufferSize) {
                    existing
                } else {
                    val newBuf = ByteArray(bufferSize)
                    deviceBuffers[device.macAddress] = newBuf
                    newBuf
                }
                
                mapDeviceToBuffer(device, data)
                sendUdp(device.ip, data)
                
            } catch (e: Exception) {
                // Ignore send errors to prevent loop crash
            }
        }
    }

    fun handleTouch(x: Float, y: Float): Boolean {
        // Iterate in reverse to hit top-most first
        val regionsCopy = synchronized(activeRegions) { activeRegions.toList() }
        
        for (region in regionsCopy.reversed()) {
            val cx = region.rect.centerX()
            val cy = region.rect.centerY()
            
            val dx = x - cx
            val dy = y - cy
            val rad = Math.toRadians(-region.rotation.toDouble())
            val cos = Math.cos(rad)
            val sin = Math.sin(rad)
            
            val rotX = (dx * cos - dy * sin).toFloat() + cx
            val rotY = (dx * sin + dy * cos).toFloat() + cy
            
            if (region.rect.contains(rotX, rotY)) {
                val localX = rotX - region.rect.left
                val localY = rotY - region.rect.top
                if (region.animation.onTouch(localX, localY)) {
                    return true
                }
            }
        }
        return false
    }

    private fun mapDeviceToBuffer(device: WledDevice, data: ByteArray) {
        // Vector-based mapping
        val rad = Math.toRadians(device.rotation.toDouble())
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        val cx = device.x + device.width / 2f
        val cy = device.y + device.height / 2f
        
        // Heuristic: Is this a Matrix?
        val aspectRatio = if (device.height > 1f) device.width / device.height else 100f
        
        // Relaxed Heurisitc:
        // 1. Explicit width set (>1)
        // 2. High pixel count (>30) and roughly rectangular (< 6:1)
        // 3. Square-ish (< 2:1) and > 9 pixels (3x3)
        val isLikelyMatrix = (device.segmentWidth > 1) ||
                             (device.pixelCount > 30 && aspectRatio < 6.0f) ||
                             (device.pixelCount > 9 && aspectRatio < 2.0f)
        
        val sampleOffsetX = -renderOriginX // Add this to WorldX to get BitmapX
        val sampleOffsetY = -renderOriginY
        
        if (isLikelyMatrix) {
            // 2D Matrix Mapping
            var cols = device.segmentWidth
            if (cols <= 0) {
                 val sqrt = kotlin.math.sqrt(device.pixelCount.toFloat())
                 if (kotlin.math.abs(sqrt - sqrt.roundToInt()) < 0.01f) {
                     cols = sqrt.roundToInt()
                 } else {
                     cols = kotlin.math.sqrt(device.pixelCount * aspectRatio).roundToInt()
                 }
                 if (cols < 1) cols = 1
            }
            val rows = (device.pixelCount + cols - 1) / cols 
            
            val stepX = if (cols > 1) device.width / (cols - 1) else 0f
            val stepY = if (rows > 1) device.height / (rows - 1) else 0f
            
            val startLocalX = -device.width / 2f
            val startLocalY = -device.height / 2f
            
            for (i in 0 until device.pixelCount) {
                val row = i / cols
                val col = i % cols
                
                val localX = startLocalX + col * stepX
                val localY = startLocalY + row * stepY
                
                val rotX = (localX * cos - localY * sin) + cx
                val rotY = (localX * sin + localY * cos) + cy
                
                val sX = (rotX + sampleOffsetX).roundToInt().coerceIn(0, bufferBitmap.width - 1)
                val sY = (rotY + sampleOffsetY).roundToInt().coerceIn(0, bufferBitmap.height - 1)
                
                val pixel = bufferBitmap.getPixel(sX, sY)
                
                data[i * 3] = (pixel shr 16 and 0xFF).toByte()
                data[i * 3 + 1] = (pixel shr 8 and 0xFF).toByte()
                data[i * 3 + 2] = (pixel and 0xFF).toByte()
            }
        } else {
            // 1D Strip Mapping
            val length = device.width 
            val step = if (device.pixelCount > 1) length / (device.pixelCount - 1) else 0f
            val startLocalX = -device.width / 2f
            
            for (i in 0 until device.pixelCount) {
                val localX = startLocalX + i * step
                val localY = 0.0 
                
                val rotX = (localX * cos - localY * sin) + cx
                val rotY = (localX * sin + localY * cos) + cy
                
                val sX = (rotX + sampleOffsetX).roundToInt().coerceIn(0, bufferBitmap.width - 1)
                val sY = (rotY + sampleOffsetY).roundToInt().coerceIn(0, bufferBitmap.height - 1)
                
                val pixel = bufferBitmap.getPixel(sX, sY)
                
                data[i * 3] = (pixel shr 16 and 0xFF).toByte()
                data[i * 3 + 1] = (pixel shr 8 and 0xFF).toByte()
                data[i * 3 + 2] = (pixel and 0xFF).toByte()
            }
        }
    }
    
    private val packetBuffer = ByteArray(1500) // MTU safe

    private fun sendUdp(ip: String, data: ByteArray) {
        try {
            // DDP Protocol Implementation
            val dataLen = data.size
            val packetLen = 10 + dataLen
            
            // Realloc if needed (unlikely for single packet < 1490 bytes)
            val buffer = if (packetLen > packetBuffer.size) ByteArray(packetLen) else packetBuffer
            
            buffer[0] = 0x41.toByte() // Ver1 + Push
            buffer[1] = 0.toByte()    // Sequence
            buffer[2] = 1.toByte()    // RGB
            buffer[3] = 1.toByte()    // Source
            
            buffer[4] = 0
            buffer[5] = 0
            buffer[6] = 0
            buffer[7] = 0
            
            buffer[8] = (dataLen shr 8).toByte()
            buffer[9] = (dataLen and 0xFF).toByte()
            
            System.arraycopy(data, 0, buffer, 10, dataLen)
            
            val address = InetAddress.getByName(ip)
            val packet = DatagramPacket(buffer, packetLen, address, 4048)
            socket.send(packet)
        } catch (e: Exception) {
            // Log.e("Engine", "Send failed", e)
        }
    }
}

interface Animation {
    fun draw(canvas: Canvas, width: Float, height: Float)
    fun onTouch(x: Float, y: Float): Boolean { return false }
}
