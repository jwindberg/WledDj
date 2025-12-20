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
    
    // Virtual buffer
    private var bufferBitmap: Bitmap = Bitmap.createBitmap(
        installation.width.toInt().coerceAtLeast(1),
        installation.height.toInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    private var canvas = Canvas(bufferBitmap)
    
    // Preview flow for UI
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap = _previewBitmap.asStateFlow()

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
        // Resize buffer if needed
        if (newInstallation.width.toInt() != bufferBitmap.width || 
            newInstallation.height.toInt() != bufferBitmap.height) {
            bufferBitmap = Bitmap.createBitmap(
                newInstallation.width.toInt().coerceAtLeast(1),
                newInstallation.height.toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
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
                
                _previewBitmap.emit(bufferBitmap.copy(Bitmap.Config.ARGB_8888, false))

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
        // Clear background
        canvas.drawColor(Color.BLACK)
        
        // Draw regions
        val regionsCopy = synchronized(activeRegions) { activeRegions.toList() }
        
        regionsCopy.forEach { region ->
            canvas.save()
            
            // 1. Rotate around region center
            canvas.rotate(region.rotation, region.rect.centerX(), region.rect.centerY())
            
            // 2. Clip to the region bounds
            canvas.clipRect(region.rect)
            
            // 3. Translate so (0,0) for the animation is the top-left of the region
            canvas.translate(region.rect.left, region.rect.top)
            
            // 4. Draw animation with region dimensions
            region.animation.draw(canvas, region.rect.width(), region.rect.height())
            
            canvas.restore()
        }
    }

    private fun mapAndSend() {
        installation.devices.forEach { device ->
            val packetData = mapDevice(device)
            sendUdp(device.ip, packetData)
        }
    }
    
    // ... mapDevice, sendUdp ...

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

    private fun mapDevice(device: WledDevice): ByteArray {
        val data = ByteArray(device.pixelCount * 3)
        
        // Vector-based mapping
        val rad = Math.toRadians(device.rotation.toDouble())
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        
        val cx = device.x + device.width / 2f
        val cy = device.y + device.height / 2f
        
        // Heuristic: Is this a Matrix?
        // Use segmentWidth if available (explicit).
        // Otherwise, if pixelCount > 40 and Aspect Ratio is < 6.0 (not a long thin strip), assume Matrix.
        val aspectRatio = if (device.height > 0) device.width / device.height else 10f
        val isLikelyMatrix = device.pixelCount > 40 && aspectRatio < 6.0f
        
        if (device.segmentWidth > 1 || isLikelyMatrix) {
            // 2D Matrix Mapping
            var cols = device.segmentWidth
            if (cols <= 0) {
                 // 1. Try Perfect Square
                 val sqrt = kotlin.math.sqrt(device.pixelCount.toFloat())
                 if (kotlin.math.abs(sqrt - sqrt.roundToInt()) < 0.01f) {
                     cols = sqrt.roundToInt()
                 } else {
                     // 2. Infer from Aspect Ratio
                     cols = kotlin.math.sqrt(device.pixelCount * aspectRatio).roundToInt()
                 }
                 if (cols < 1) cols = 1
            }
            // Ensure we don't divide by zero if something weird happens
            val rows = (device.pixelCount + cols - 1) / cols 
            
            // Steps
            val stepX = if (cols > 1) device.width / (cols - 1) else 0f
            val stepY = if (rows > 1) device.height / (rows - 1) else 0f
            
            val startLocalX = -device.width / 2f
            val startLocalY = -device.height / 2f
            
            for (i in 0 until device.pixelCount) {
                // Determine Row/Col (Assumes Standard Row-Major Layout handling)
                val row = i / cols
                val col = i % cols
                
                // Serpentine Handling: disabled by default as it breaks Standard matrices
                // if (row % 2 != 0) col = cols - 1 - col
                
                val localX = startLocalX + col * stepX
                val localY = startLocalY + row * stepY
                
                // Rotate
                val rotX = (localX * cos - localY * sin) + cx
                val rotY = (localX * sin + localY * cos) + cy
                
                val sX = rotX.roundToInt().coerceIn(0, bufferBitmap.width - 1)
                val sY = rotY.roundToInt().coerceIn(0, bufferBitmap.height - 1)
                
                val pixel = bufferBitmap.getPixel(sX, sY)
                
                data[i * 3] = (pixel shr 16 and 0xFF).toByte()
                data[i * 3 + 1] = (pixel shr 8 and 0xFF).toByte()
                data[i * 3 + 2] = (pixel and 0xFF).toByte()
            }
        } else {
            // 1D Strip Mapping (Existing Logic)
            val length = device.width 
            val step = if (device.pixelCount > 1) length / (device.pixelCount - 1) else 0f
            val startLocalX = -device.width / 2f
            
            for (i in 0 until device.pixelCount) {
                val localX = startLocalX + i * step
                val localY = 0.0 
                
                val rotX = (localX * cos - localY * sin) + cx
                val rotY = (localX * sin + localY * cos) + cy
                
                val sX = rotX.roundToInt().coerceIn(0, bufferBitmap.width - 1)
                val sY = rotY.roundToInt().coerceIn(0, bufferBitmap.height - 1)
                
                val pixel = bufferBitmap.getPixel(sX, sY)
                
                data[i * 3] = (pixel shr 16 and 0xFF).toByte()
                data[i * 3 + 1] = (pixel shr 8 and 0xFF).toByte()
                data[i * 3 + 2] = (pixel and 0xFF).toByte()
            }
        }
        
        // Using DRGB (2):
        // Header is just byte 2, byte timeout.
        val packet = ByteArray(2 + data.size)
        packet[0] = 2.toByte() // DRGB
        packet[1] = 2.toByte() // Timeout seconds
        System.arraycopy(data, 0, packet, 2, data.size)
        
        return packet
    }

    private fun sendUdp(ip: String, data: ByteArray) {
        try {
            val address = InetAddress.getByName(ip)
            val packet = DatagramPacket(data, data.size, address, 21324)
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
