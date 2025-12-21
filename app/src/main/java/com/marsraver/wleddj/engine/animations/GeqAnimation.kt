package com.marsraver.wleddj.engine.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.math.max

/**
 * GEQ (Graphic Equalizer) animation - 2D frequency band visualization.
 * Adapted for Android Canvas.
 */
class GeqAnimation : Animation {

    private var fftMeter: FftMeter? = null
    
    // Internal state
    // We'll use a fixed internal resolution for the simulation grid
    private val internalWidth = 32
    private val internalHeight = 32
    
    private var pixelColors: IntArray? = null // flattened array for bitmap
    private var previousBarHeight: IntArray = IntArray(internalWidth)
    
    private var lastRippleTime: Long = 0
    private var callCount: Long = 0
    
    // Config
    private var numBands: Int = 16
    private var noiseFloor: Int = 20
    private var paramSpeed = 128 // Fade speed
    private var paramIntensity = 128 // Ripple speed
    
    private var internalBitmap: Bitmap? = null
    private val paint = Paint().apply { isFilterBitmap = false } // Pixelated look preferred?

    init {
        init()
    }

    private fun init() {
        pixelColors = IntArray(internalWidth * internalHeight) { Color.BLACK }
        previousBarHeight = IntArray(internalWidth)
        lastRippleTime = 0
        callCount = 0
        fftMeter = FftMeter(bands = 16)
        
        internalBitmap = Bitmap.createBitmap(internalWidth, internalHeight, Bitmap.Config.ARGB_8888)
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val now = System.nanoTime()
        update(now)
        
        // Render internal state to bitmap
        pixelColors?.let { colors ->
            // Safety check
            if (internalBitmap != null && !internalBitmap!!.isRecycled) {
                internalBitmap?.setPixels(colors, 0, internalWidth, 0, 0, internalWidth, internalHeight)
            }
        }
        
        // Draw scaled
        internalBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, RectF(0f, 0f, width, height), paint)
        }
    }

    private fun update(now: Long) {
        callCount++
        
        val spectrumSnapshot = fftMeter?.getNormalizedBands() ?: IntArray(16)

        var rippleTime = false
        var rippleInterval = 256 - paramIntensity
        if (rippleInterval < 1) rippleInterval = 1
        
        // Convert ripple interval to nanos (approx) - original was seemingly arbitrary units or ms
        // Let's assume param 128 ~ 30ms? 
        // 128 * 1ms = 128ms. 
        if (now - lastRippleTime >= rippleInterval * 500_000L) { // Adjusted heuristic
            lastRippleTime = now
            rippleTime = true
        }

        // Fade Logic
        // Original: if (fadeoutDelay <= 1 || callCount % fadeoutDelay == 0)
        // We run ~60fps.
        // Let's simplified fade every frame
        fadeToBlack(paramSpeed / 10) // Adjustment for 60fps

        for (x in 0 until internalWidth) {
            // Map X column to Freq Band
            var band = mapValue(x, 0, internalWidth, 0, numBands)
            band = band.coerceIn(0, 15)

            // Get FFT Value
            val bandValue = spectrumSnapshot.getOrElse(band) { 0 }
            val effectiveValue = max(0, bandValue - noiseFloor)
            
            // Map FFT value to Height
            var barHeight = mapValue(effectiveValue, 0, 255 - noiseFloor, 0, internalHeight)
            barHeight = barHeight.coerceIn(0, internalHeight)

            // Peak Hold
            if (barHeight > previousBarHeight[x]) {
                previousBarHeight[x] = barHeight
            }

            // Draw Bar
            // Color based on Band (Horizontal Rainbow)
            val colorIndex = (band * 16).coerceIn(0, 255)
            val ledColor = getColorFromPalette(colorIndex)

            for (y in 0 until barHeight) {
                // Drawing from bottom up ??
                // Array y=0 is top typically.
                // Let's assume y=0 is BOTTOM for visual EQ logic
                // So setPixel(x, internalHeight - 1 - y)
                 setPixelColor(x, internalHeight - 1 - y, ledColor)
            }

            // Draw Peak Dot
            if (previousBarHeight[x] > 0) {
                 val peakY = (internalHeight - previousBarHeight[x]).coerceIn(0, internalHeight - 1)
                 setPixelColor(x, peakY, Color.WHITE)
            }

            // Gravity for Peak
            if (rippleTime && previousBarHeight[x] > 0) {
                previousBarHeight[x]--
            }
        }
    }

    private fun fadeToBlack(fadeAmount: Int) {
        val amount = fadeAmount.coerceIn(0, 255)
        if (amount == 0) return
        val scale = (255 - amount) / 255.0f
        
        for (i in pixelColors!!.indices) {
            val c = pixelColors!![i]
            if (c != Color.BLACK) {
                val a = Color.alpha(c)
                val r = (Color.red(c) * scale).toInt()
                val g = (Color.green(c) * scale).toInt()
                val b = (Color.blue(c) * scale).toInt()
                pixelColors!![i] = Color.argb(a, r, g, b)
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, color: Int) {
        if (x in 0 until internalWidth && y in 0 until internalHeight) {
            pixelColors!![y * internalWidth + x] = color
        }
    }
    
    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
    
    private fun getColorFromPalette(index: Int): Int {
        // Simple Rainbow Palette
        val h = index / 255.0f * 360.0f
        return Color.HSVToColor(floatArrayOf(h, 1.0f, 1.0f))
    }
    
    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
        internalBitmap?.recycle()
        internalBitmap = null
        pixelColors = null
    }
}
