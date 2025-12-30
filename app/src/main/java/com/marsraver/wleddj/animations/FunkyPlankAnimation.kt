package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.FftMeter
import com.marsraver.wleddj.engine.color.Palette

/**
 * Funky Plank Animation - Spectral Waterfall.
 * Smoothly scrolling history of FFT bands.
 */
class FunkyPlankAnimation : Animation {

    private var _palette: Palette = Palette.RAINBOW
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // State
    private var fftMeter: FftMeter? = null
    private var isInit = false
    
    // History
    // Store arrays of floats (normalized 0..1)? Or Ints (0..255)
    // Let's store Ints 0..255 for simplicity with colors
    private val fftHistory = ArrayDeque<IntArray>()
    private val HISTORY_SIZE = 100 // How many rows to keep
    private val NUM_BANDS = 16
    
    private val paint = Paint().apply { isAntiAlias = false } // Rects don't need AA if packed tight
    private val rect = RectF()

    // Params
    private var paramSpeed: Int = 128
    private var paramIntensity: Int = 128
    
    private var lastUpdate: Long = 0

    private fun init() {
        if (!isInit) {
            fftMeter = FftMeter(bands = NUM_BANDS)
            isInit = true
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        init()
        
        // Update History
        val now = System.currentTimeMillis()
        // Speed determines how often we sample a new line
        // 0 -> Slow (every 100ms)
        // 255 -> Fast (every 10ms)
        val interval = 100 - (paramSpeed / 255f) * 90
        
        if (now - lastUpdate > interval) {
            val bands = fftMeter?.getNormalizedBands() ?: IntArray(NUM_BANDS)
            // Copy because getNormalizedBands might return same array reference (depends on impl)? 
            // Checking FftMeter impl: it returns `outBuffer` clone or new array usually. 
            // In typical Android Fft it reuses. Let's clone to be safe.
            fftHistory.addFirst(bands.clone())
            if (fftHistory.size > HISTORY_SIZE) {
                fftHistory.removeLast()
            }
            lastUpdate = now
        }
        
        // Clear
        canvas.drawColor(Color.BLACK)
        
        // Draw Waterfall
        // Rows go down? Or Up? 
        // Let's scroll Down (Newest at top).
        
        val rowHeight = height / HISTORY_SIZE.toFloat()
        // Or better: fit history to height? 
        // If we want it to look "continuous", rowHeight should cover screen.
        // Actually, let's fix row height calculation to ensure clean coverage.
        
        // Optimization: Draw rects
        paint.style = Paint.Style.FILL
        
        fftHistory.forEachIndexed { rowIndex, bands ->
             val y = rowIndex * rowHeight
             val bandWidth = width / NUM_BANDS.toFloat()
             
             for (i in 0 until NUM_BANDS) {
                 val mag = bands.getOrElse(i) { 0 } // 0..255 (normalized usually returns high values?)
                 // Wait, getNormalizedBands usually returns something appropriate. 
                 // Assuming 0..255-ish range from previous code usage.
                 
                 // Threshold
                 if (mag > 10) {
                     val x = i * bandWidth
                     
                     // Color:
                     // Index in palette based on Band + Magnitude?
                     // Or just Band?
                     // Let's use Band + Time (scrolling color?) or Band + Mag.
                     // FunkyPlank used: (colorIndex * 255 / numBands)
                     
                     val colorIdx = (i * 16) + (mag / 2)
                     val color = _palette.getInterpolatedInt(colorIdx % 256)
                     
                     // Brightness based on mag
                     // Just use Color but modulate alpha/brightness?
                     // Palette handles color. Let's assume Palette color is bright enough.
                     paint.color = color
                     
                     // Rect
                     rect.set(x, y, x + bandWidth, y + rowHeight + 0.5f) // +0.5 to avoid gaps
                     canvas.drawRect(rect, paint)
                 }
             }
        }
    }

    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
    }
}
