package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.*
import kotlin.random.Random

/**
 * Blurz animation - Random pixels with blur effect reactive to sound.
 * Adapted for Android Canvas.
 */
class BlurzAnimation : Animation {

    private var _palette: Palette = Palette.RAINBOW
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    private var pixelBrightness: Array<ByteArray>? = null
    private var pixelHue: Array<ByteArray>? = null
    private var currentHue: Int = 0
    private var loudnessMeter: LoudnessMeter? = null
    
    // Internal buffer for pixel manipulation (low res)
    private var internalBitmap: Bitmap? = null
    private var internalCanvas: Canvas? = null
    private val paint = Paint()
    
    // Config
    private val internalWidth = 50 
    private val internalHeight = 50
    
    private var lastPixelTime: Long = 0
    private var lastFadeTime: Long = 0
    
    private val RANDOM = Random.Default
    
    // Palette Helper (Hue acts as Index 0-255)
    private fun getColorFromPalette(hue: Int): Int {
        return _palette.getInterpolatedInt(hue)
    }

    init {
        init()
    }

    private fun init() {
        pixelBrightness = Array(internalWidth) { ByteArray(internalHeight) }
        pixelHue = Array(internalWidth) { ByteArray(internalHeight) }
        lastPixelTime = 0
        lastFadeTime = 0
        loudnessMeter = LoudnessMeter()
        
        internalBitmap = Bitmap.createBitmap(internalWidth, internalHeight, Bitmap.Config.ARGB_8888)
        internalCanvas = Canvas(internalBitmap!!)
    }

    override fun onTouch(x: Float, y: Float): Boolean {
        // ...
        return false
    }
    
    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val now = System.nanoTime()
        update(now)
        
        // Draw the internal low-res bitmap to the high-res external canvas
        // This gives the "pixelated" look if we use Nearest Neighbor, or smooth if Filter is true.
        // For "Blurz", smoothness might be desired, or maybe pixelated.
        // Let's assume smooth scaling triggers the "blur" effect naturally?
        // Actually the original Blurz logic does fading/blurring in the array.
        
        // 1. Render Arrays to Bitmap
        val bitmap = internalBitmap ?: return
        
        // This is slow if done pixel by pixel in Kotlin on every frame.
        // Optimizing: Lock pixels? Not easily in Android without NDK.
        // We will just set pixels on the bitmap. 50x50 = 2500 pixels. Fast enough.
        val pixels = IntArray(internalWidth * internalHeight)
        
        for (y in 0 until internalHeight) {
            for (x in 0 until internalWidth) {
                val brightness = pixelBrightness!![x][y].toInt() and 0xFF
                if (brightness > 0) {
                    val hue = pixelHue!![x][y].toInt() and 0xFF
                    val color = getColorFromPalette(hue)
                    
                    // Apply brightness
                    // Android Color doesn't have easy scale, use HSV or RGB math
                    val r = (Color.red(color) * brightness / 255)
                    val g = (Color.green(color) * brightness / 255)
                    val b = (Color.blue(color) * brightness / 255)
                    
                    pixels[y * internalWidth + x] = Color.rgb(r, g, b)
                } else {
                    pixels[y * internalWidth + x] = Color.TRANSPARENT
                }
            }
        }
        
        bitmap.setPixels(pixels, 0, internalWidth, 0, 0, internalWidth, internalHeight)
        
        // 2. Draw scaled bitmap
        canvas.drawBitmap(bitmap, null, android.graphics.RectF(0f, 0f, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun update(now: Long) {
        // Time logic: Original used explicit timestamps. 
        // We can approximate frame deltas.
        
        // Fade Logic (every ~100ms in original?)
        // original: if (now - lastFadeTime > 100_000_000L)
        if (now - lastFadeTime > 50_000_000L) { // Speed up slightly for 60fps
             val fadeSpeed = 10 // Slower fade for smoother trail
             for (x in 0 until internalWidth) {
                for (y in 0 until internalHeight) {
                    val brightness = (pixelBrightness!![x][y].toInt() and 0xFF)
                    if (brightness > 0) {
                        val updated = max(0, brightness - fadeSpeed)
                        pixelBrightness!![x][y] = updated.toByte()
                    }
                }
             }
             lastFadeTime = now
        }
        
        // Spawn Logic (every ~50ms)
        if (now - lastPixelTime > 30_000_000L) {
            val loudness = loudnessMeter?.getNormalizedLoudness() ?: 0 // Use AGC Normalized value
            val levelSnapshot = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
            val threshold = 5 // Much lower threshold for sensitivity
            
            if (levelSnapshot < threshold) return
            
            val spikeAmount = levelSnapshot - threshold
            val adjustedBrightness = min(255, max(100, spikeAmount * 5))
            
            currentHue = (currentHue + 5) % 255
            val pixelsPerFrame = max(1, levelSnapshot / 20)
            
            repeat(pixelsPerFrame) {
                val randomX = RANDOM.nextInt(internalWidth)
                val randomY = RANDOM.nextInt(internalHeight)
                val spotRadius = 1 + RANDOM.nextInt(2)

                for (dx in -spotRadius..spotRadius) {
                    for (dy in -spotRadius..spotRadius) {
                        val px = randomX + dx
                        val py = randomY + dy
                        val distance = sqrt((dx * dx + dy * dy).toDouble())
                        if (px in 0 until internalWidth && py in 0 until internalHeight && distance <= spotRadius) {
                            val fadeFactor = 1.0 - (distance / spotRadius)
                            val spotBrightness = (adjustedBrightness * fadeFactor).roundToInt()
                            val existing = pixelBrightness!![px][py].toInt() and 0xFF
                            if (spotBrightness > existing) {
                                pixelBrightness!![px][py] = spotBrightness.coerceIn(0, 255).toByte()
                                pixelHue!![px][py] = currentHue.toByte()
                            }
                        }
                    }
                }
            }
            lastPixelTime = now
        }
    }
    
    // Cleanup? Animation interface doesn't have explicit destroy.
    // Ideally we should stop the meter.
    // We might need to add onRemove/onDestroy to Animation interface later.
    // For now, finalize via GC? LoudnessMeter uses coroutine scope supervisor.
    // AudioPipeline is singleton flow, LoudnessMeter subscribes.
    // LoudnessMeter.stop() cancels scope.
}
