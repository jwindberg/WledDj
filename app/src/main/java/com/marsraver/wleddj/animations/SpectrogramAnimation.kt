package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.math.max

/**
 * Spectrogram Animation (Waterfall)
 * Scrolls audio frequency history over time.
 * X-Axis: Frequency (Low -> High)
 * Color Brightness: Amplitude
 */
class SpectrogramAnimation : Animation {

    private var fftMeter: FftMeter? = null
    
    // Internal resolution
    private val internalWidth = 64
    private val internalHeight = 64
    
    private var internalBitmap: Bitmap? = null
    private val paint = Paint().apply { isFilterBitmap = false } // Pixelated look
    
    // Temp buffer for scrolling
    private val scrollSrcRect = Rect(0, 1, internalWidth, internalHeight)
    private val scrollDstRect = Rect(0, 0, internalWidth, internalHeight - 1)
    
    init {
        init()
    }

    private fun init() {
        fftMeter = FftMeter(bands = internalWidth) // One band per pixel column
        internalBitmap = Bitmap.createBitmap(internalWidth, internalHeight, Bitmap.Config.ARGB_8888)
        internalBitmap?.eraseColor(Color.BLACK)
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        update()
        
        internalBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, RectF(0f, 0f, width, height), paint)
        }
    }

    private fun update() {
        val bitmap = internalBitmap ?: return
        
        // 1. Scroll Up
        // Creating a temporary bitmap is expensive every frame.
        // Better: Draw bitmap onto itself shifted? 
        // Android Canvas extraction?
        // Actually, Bitmap.createBitmap(source, ...) is copy.
        // Efficient scroll:
        // Use a Canvas wrapper around the Bitmap.
        val c = Canvas(bitmap)
        
        // We want to shift everything UP by 1 pixel.
        // We copy [0, 1, w, h] to [0, 0, w, h-1].
        // But we can't easily do block copy within same bitmap without artifacting if overlapping?
        // Actually `drawBitmap` handles overlap correctly usually?
        // Issue: We are modifying the bitmap we are drawing to.
        // Safe approach: Move row by row? Too slow.
        // Alternative: Circular buffer?
        // Alternative: Use an IntArray buffer, shift array (System.arraycopy), updates bitmap.
        // IntArray size 64x64 = 4096 ints. Tiny. Fast.
        
        shiftPixelsAndAddRow(bitmap)
    }
    
    private var pixelBuffer: IntArray? = null
    
    private fun shiftPixelsAndAddRow(bitmap: Bitmap) {
        if (pixelBuffer == null) {
            pixelBuffer = IntArray(internalWidth * internalHeight)
            bitmap.getPixels(pixelBuffer!!, 0, internalWidth, 0, 0, internalWidth, internalHeight)
        }
        val pixels = pixelBuffer!!
        
        // Shift Up: Copy rows 1..H-1 to 0..H-2
        // Destination Start: 0
        // Source Start: internalWidth
        // Length: (H-1) * W
        System.arraycopy(pixels, internalWidth, pixels, 0, (internalHeight - 1) * internalWidth)
        
        // Generate New Row at Bottom
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(internalWidth)
        // bands size might not match internalWidth if FftMeter bands fixed?
        // FftMeter constructor allows setting bands.
        
        val rowStart = (internalHeight - 1) * internalWidth
        
        for (x in 0 until internalWidth) {
            // Get band value (0-255)
            // FftMeter bands might be less than internalWidth if it clamps?
            // Let's assume 1:1 since we requested it.
            val bandVal = if (x < bands.size) bands[x] else 0
            
            // Map to Color
            // Hue: X position (Rainbow)
            // Value: Band Amplitude
            // Saturation: Full
            
            val hue = (x.toFloat() / internalWidth) * 360f
            val value = bandVal / 255f
            
            val color = if (value > 0.05f) { // Noise gate
                Color.HSVToColor(floatArrayOf(hue, 1f, value))
            } else {
                Color.BLACK
            }
            
            pixels[rowStart + x] = color
        }
        
        // Write back to Bitmap
        bitmap.setPixels(pixels, 0, internalWidth, 0, 0, internalWidth, internalHeight)
    }

    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
        internalBitmap?.recycle()
        internalBitmap = null
        pixelBuffer = null
    }
}
