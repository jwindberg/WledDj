package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.Animation
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Soap animation - smooth flowing perlin noise with cross-axis blending.
 * Adapted for Android Canvas.
 */
class SoapAnimation : Animation {

    private var pixels: IntArray = IntArray(0)
    private var noiseBuffer: Array<DoubleArray> = emptyArray()
    private var rowBlend: DoubleArray = DoubleArray(0)
    private var colBlend: DoubleArray = DoubleArray(0)

    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var offsetZ: Double = 0.0
    private var scaleX: Double = 1.0
    private var scaleY: Double = 1.0

    // Internal resolution for simulation (to keep it fast)
    private val simWidth = 64
    private val simHeight = 64
    
    private var bitmap: Bitmap? = null
    private val paint = Paint()
    private val destRect = Rect()

    private var isInitialized = false
    
    // Params
    private var paramSpeed = 128
    
    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        paramSpeed = (speed * 255f).toInt().coerceIn(0, 255)
    }
    override fun getSpeed(): Float = paramSpeed / 255f
    
    private fun initSoap() {
        // We use fixed simulation size
        val width = simWidth
        val height = simHeight

        pixels = IntArray(width * height)
        noiseBuffer = Array(width) { DoubleArray(height) }
        rowBlend = DoubleArray(height)
        colBlend = DoubleArray(width)

        // Init scale based on sim size
        scaleX = 1.6 / width
        scaleY = 1.6 / height

        offsetX = Random.nextDouble(0.0, 10_000.0)
        offsetY = Random.nextDouble(0.0, 10_000.0)
        offsetZ = Random.nextDouble(0.0, 10_000.0)
        
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        isInitialized = true
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        if (!isInitialized) {
            initSoap()
        }
        
        // Update logic
        updateSoap()
        
        // Render to Bitmap
        bitmap?.let { bmp ->
            bmp.setPixels(pixels, 0, simWidth, 0, 0, simWidth, simHeight)
            
            // Draw Bitmap scaled to Canvas
            destRect.set(0, 0, width.toInt(), height.toInt())
            canvas.drawBitmap(bmp, null, destRect, paint)
        }
    }

    private fun updateSoap() {
        val width = simWidth
        val height = simHeight
        
        // Logic adapted from original SoapAnimation
        val baseScale = max(1, min(width, height))
        val movementFactor = baseScale * 0.0004
        val speedMult = paramSpeed / 128.0

        offsetX += movementFactor * speedMult
        offsetY += movementFactor * 0.87 * speedMult
        offsetZ += movementFactor * 1.13 * speedMult

        val smoothness = 200

        // Compute noise field with exponential smoothing.
        for (x in 0 until width) {
            val iOffset = scaleX * (x - width / 2)
            for (y in 0 until height) {
                val jOffset = scaleY * (y - height / 2)
                val noise = MathUtils.perlinNoise(offsetX + iOffset, offsetY + jOffset, offsetZ)
                val value = ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0)
                val previous = noiseBuffer[x][y]
                val blended = (previous * smoothness + value * (255 - smoothness)) / 255.0
                noiseBuffer[x][y] = blended
            }
        }

        // Pre-compute row and column blends for soap-like diffusion.
        for (y in 0 until height) {
            var sum = 0.0
            for (x in 0 until width) {
                sum += noiseBuffer[x][y]
            }
            rowBlend[y] = sum / width
        }
        for (x in 0 until width) {
            var sum = 0.0
            for (y in 0 until height) {
                sum += noiseBuffer[x][y]
            }
            colBlend[x] = sum / height
        }

        // Update pixels combining local value with row/column blends.
        for (x in 0 until width) {
            for (y in 0 until height) {
                val value = noiseBuffer[x][y]
                val blended = (value * 0.6) + (rowBlend[y] * 0.2) + (colBlend[x] * 0.2)
                val hue = ((255 - blended) * 3).roundToInt() and 0xFF
                // Map 0-255 hue to 0-360
                val hsvHue = (hue / 255.0f) * 360.0f
                
                val sat = (200 + (value / 255.0) * 40).roundToInt().coerceIn(0, 255) / 255.0f
                val bri = (180 + (blended / 255.0) * 75).roundToInt().coerceIn(0, 255) / 255.0f

                pixels[y * width + x] = Color.HSVToColor(floatArrayOf(hsvHue, sat, bri))
            }
        }
    }
}
