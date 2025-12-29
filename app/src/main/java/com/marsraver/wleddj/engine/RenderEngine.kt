package com.marsraver.wleddj.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.marsraver.wleddj.model.AnimationRegion
import com.marsraver.wleddj.model.Installation
import com.marsraver.wleddj.model.WledDevice
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


    private val lock = Any()
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

    // Animations (Regions)
    private val activeRegions = mutableListOf<AnimationRegion>()

    init {
        recalculateBounds()
    }



    fun addRegion(region: AnimationRegion) {
        synchronized(lock) {
            activeRegions.add(region)
            recalculateBounds()
        }
    }

    fun clearAnimations() {
        synchronized(lock) {
            activeRegions.forEach { it.animation.destroy() }
            activeRegions.clear()
            recalculateBounds()
        }
    }
    
    fun getRegions(): List<AnimationRegion> {
        synchronized(lock) {
            return activeRegions.toList()
        }
    }

    fun updateRegion(id: String, newRect: RectF, newRotation: Float) {
        synchronized(lock) {
            val index = activeRegions.indexOfFirst { it.id == id }
            if (index != -1) {
                activeRegions[index] = activeRegions[index].copy(rect = newRect, rotation = newRotation)
                recalculateBounds()
            }
        }
    }
    
    fun removeRegion(id: String) {
        synchronized(lock) {
            val iter = activeRegions.iterator()
            while (iter.hasNext()) {
                val region = iter.next()
                if (region.id == id) {
                    region.animation.destroy()
                    iter.remove()
                }
            }
            recalculateBounds()
            recalculateBounds()
        }
    }

    fun bringToFront(id: String) {
        synchronized(lock) {
            val index = activeRegions.indexOfFirst { it.id == id }
            if (index != -1 && index != activeRegions.lastIndex) {
                val region = activeRegions.removeAt(index)
                activeRegions.add(region)
                // Bounds don't typically change on reorder, but safe to keep inconsistent state away
            }
        }
    }

    // Network
    private val udpClient = com.marsraver.wleddj.wled.WledUdpClient()

    fun updateInstallation(newInstallation: Installation) {
        synchronized(lock) {
            installation = newInstallation
            recalculateBounds()
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
                
                // Be careful extracting bitmap under lock vs emitting outside
                val frame = synchronized(lock) {
                     PreviewFrame(
                        bufferBitmap.copy(Bitmap.Config.ARGB_8888, false),
                        renderOriginX,
                        renderOriginY
                    )
                }
                _previewFrame.emit(frame)

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
        synchronized(lock) {
            canvas.drawColor(Color.BLACK)
            
            // Global Transform: Translate World -> Bitmap Space
            canvas.save()
            canvas.translate(-renderOriginX, -renderOriginY)
            
            // Draw regions
            // No need to copy list if we are under lock and iterating
            activeRegions.forEach { region ->
                canvas.save()
                canvas.rotate(region.rotation, region.rect.centerX(), region.rect.centerY())
                canvas.clipRect(region.rect)
                canvas.translate(region.rect.left, region.rect.top)
                region.animation.draw(canvas, region.rect.width(), region.rect.height())
                canvas.restore()
            }
            canvas.restore() 
        }
    }

    private fun recalculateBounds() {
        synchronized(lock) {
            // Union of Canvas (0,0 -> W,H) and all Devices AND Regions
            var minX = 0f
            var minY = 0f
            var maxX = installation.width
            var maxY = installation.height
            
            // 1. Devices
            installation.devices.forEach { device ->
                // Calculate 4 corners of the rotated device
                val cx = device.x + device.width / 2f
                val cy = device.y + device.height / 2f
                val rad = Math.toRadians(device.rotation.toDouble())
                val cos = Math.cos(rad)
                val sin = Math.sin(rad)
                
                val w2 = device.width / 2f
                val h2 = device.height / 2f
                
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
            
            // 2. Regions
            // We are under lock, direct iteration safe
            activeRegions.forEach { region ->
                val cx = region.rect.centerX()
                val cy = region.rect.centerY()
                val rad = Math.toRadians(region.rotation.toDouble())
                val cos = Math.cos(rad)
                val sin = Math.sin(rad)
                
                val w2 = region.rect.width() / 2f
                val h2 = region.rect.height() / 2f
                
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
            val padding = 100f // Increased padding for safety
            minX -= padding
            minY -= padding
            maxX += padding
            maxY += padding
    
            renderBounds.set(minX, minY, maxX, maxY)
            renderOriginX = minX
            renderOriginY = minY
            
            val newW = (maxX - minX).toInt().coerceAtLeast(1)
            val newH = (maxY - minY).toInt().coerceAtLeast(1)
            
            if (newW != bufferBitmap.width || newH != bufferBitmap.height) {
                bufferBitmap = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888)
                canvas = Canvas(bufferBitmap)
            }
        }
    }

    private val deviceBuffers = mutableMapOf<String, ByteArray>()

    private suspend fun mapAndSend() {
        val packetsToSend = mutableListOf<Pair<String, ByteArray>>()
        
        synchronized(lock) {
             val install = installation 
             install.devices.forEach { device ->
                try {
                    val bufferSize = device.pixelCount * 3
                    val existing = deviceBuffers[device.macAddress] 
                    val data = if (existing != null && existing.size == bufferSize) {
                        existing
                    } else {
                        val newBuf = ByteArray(bufferSize)
                        deviceBuffers[device.macAddress] = newBuf
                        newBuf
                    }
                    
                    mapDeviceToBuffer(device, data) // Uses bufferBitmap
                    packetsToSend.add(device.ip to data)
                    
                } catch (e: Exception) {
                }
            }
        }
        
        // Send outside lock
        packetsToSend.forEach { (ip, data) ->
            try {
                udpClient.sendFrame(ip, data)
            } catch (e: Exception) {
                // Ignore send errors
            }
        }
    }
    
    fun handleTouch(x: Float, y: Float): Boolean {
        synchronized(lock) {
            // Iterate in reverse
             for (region in activeRegions.reversed()) {
                 // ... logic ...
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
        }
        return false
    }

    private fun mapDeviceToBuffer(device: WledDevice, data: ByteArray) {
        val rad = Math.toRadians(device.rotation.toDouble())
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        val cx = device.x + device.width / 2f
        val cy = device.y + device.height / 2f
        
        val aspectRatio = if (device.height > 1f) device.width / device.height else 100f
        
        val isLikelyMatrix = (device.segmentWidth > 1) ||
                             (device.pixelCount > 30 && aspectRatio < 6.0f) ||
                             (device.pixelCount > 9 && aspectRatio < 2.0f)
        
        val sampleOffsetX = -renderOriginX 
        val sampleOffsetY = -renderOriginY
        
        val w = bufferBitmap.width
        val h = bufferBitmap.height
        
        if (isLikelyMatrix) {
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
                
                val sX = (rotX + sampleOffsetX).roundToInt().coerceIn(0, w - 1)
                val sY = (rotY + sampleOffsetY).roundToInt().coerceIn(0, h - 1)
                
                val pixel = bufferBitmap.getPixel(sX, sY)
                
                data[i * 3] = (pixel shr 16 and 0xFF).toByte()
                data[i * 3 + 1] = (pixel shr 8 and 0xFF).toByte()
                data[i * 3 + 2] = (pixel and 0xFF).toByte()
            }
        } else {
            val length = device.width 
            val step = if (device.pixelCount > 1) length / (device.pixelCount - 1) else 0f
            val startLocalX = -device.width / 2f
            
            for (i in 0 until device.pixelCount) {
                val localX = startLocalX + i * step
                val localY = 0.0 
                
                val rotX = (localX * cos - localY * sin) + cx
                val rotY = (localX * sin + localY * cos) + cy
                
                val sX = (rotX + sampleOffsetX).roundToInt().coerceIn(0, w - 1)
                val sY = (rotY + sampleOffsetY).roundToInt().coerceIn(0, h - 1)
                
                val pixel = bufferBitmap.getPixel(sX, sY)
                
                data[i * 3] = (pixel shr 16 and 0xFF).toByte()
                data[i * 3 + 1] = (pixel shr 8 and 0xFF).toByte()
                data[i * 3 + 2] = (pixel and 0xFF).toByte()
            }
        }
    }

    fun handleTransform(targetX: Float, targetY: Float, panX: Float, panY: Float, zoom: Float, rotation: Float): Boolean {
        synchronized(lock) {
            // Forward transform to checking regions
             for (region in activeRegions.reversed()) {
                 // Check if target point is in region
                 val cx = region.rect.centerX()
                 val cy = region.rect.centerY()
                 
                 // Transform target point to local space to check bounds
                 val dx = targetX - cx
                 val dy = targetY - cy
                 val rad = Math.toRadians(-region.rotation.toDouble())
                 val cos = Math.cos(rad)
                 val sin = Math.sin(rad)
                 val rotX = (dx * cos - dy * sin).toFloat() + cx
                 val rotY = (dx * sin + dy * cos).toFloat() + cy
                 
                 if (region.rect.contains(rotX, rotY)) {
                     // Pass through. Note: Pan is in global SCREEN pixels (or virtual pixels if scaled)
                     // Rotation is degrees. Zoom is scale factor multiplier.
                     if (region.animation.onTransform(panX, panY, zoom, rotation)) {
                         return true
                     }
                 }
             }
        }
        return false
    }
}


