package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

/**
 * Hiphotic Animation - Hypnotic Plasma.
 * Renders smooth plasma interference patterns.
 */
class HiphoticAnimation : Animation {

    private var _palette: Palette = Palettes.get("Rainbow") ?: Palettes.getDefault()
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // State
    // We render to a fixed low-res buffer for performance and "smoothness" when scaled up
    private val RENDER_WIDTH = 80
    private val RENDER_HEIGHT = 80
    
    private var buffer: Bitmap? = null
    private val paint = Paint().apply { 
        isAntiAlias = true 
        isFilterBitmap = true // Bilinear scaling
    }
    private val destRect = Rect()

    // Params
    private var paramSpeed: Int = 128
    private var paramIntensity: Int = 128
    
    private var time: Double = 0.0

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // 1. Init Buffer
        if (buffer == null) {
            buffer = Bitmap.createBitmap(RENDER_WIDTH, RENDER_HEIGHT, Bitmap.Config.ARGB_8888)
        }
        val buf = buffer ?: return
        
        // 2. Update Time
        val speed = 0.05 + (paramSpeed / 255.0) * 0.2
        time += speed
        
        // 3. Render Plasma to Buffer
        // We manipulate pixels directly for speed on this small buffer
        // (80x80 = 6400 pixels, vastly faster than 1080p)
        val pixels = IntArray(RENDER_WIDTH * RENDER_HEIGHT)
        
        val a = time
        val scale = 0.1 + (paramIntensity / 255.0) * 0.2
        
        // Optimize: Precalculate loop invariants
        for (y in 0 until RENDER_HEIGHT) {
            val yFloat = y.toFloat()
            for (x in 0 until RENDER_WIDTH) {
                val xFloat = x.toFloat()
                
                // Plasma Calc mimics original Hiphotic logic but float
                // Original: 
                // cosArg = x*speed + a/3
                // sinArg = y*intensity + a/4
                // final = cos + sin + a
                
                // New: smooth float waves
                val v1 = sin(xFloat * scale + time)
                val v2 = cos(yFloat * scale + time * 0.8)
                val v3 = sin((xFloat + yFloat) * scale * 0.5 + time * 1.2)
                
                val valSum = (v1 + v2 + v3) // Range approx -3..3
                
                // Map to 0-255 for color index
                // (valSum + 3) / 6 * 255
                val index = ((valSum + 3.0) / 6.0 * 255.0).toInt().coerceIn(0, 255)
                
                // Rotate palette over time
                val rotIndex = (index + (time * 50).toInt()) % 256
                
                pixels[y * RENDER_WIDTH + x] = _palette.getInterpolatedInt(rotIndex)
            }
        }
        
        buf.setPixels(pixels, 0, RENDER_WIDTH, 0, 0, RENDER_WIDTH, RENDER_HEIGHT)
        
        // 4. Draw Scaled Up
        destRect.set(0, 0, width.toInt(), height.toInt())
        canvas.drawBitmap(buf, null, destRect, paint)
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
    }
}
