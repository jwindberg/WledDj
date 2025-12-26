package com.marsraver.wleddj.engine.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import kotlin.math.cos
import kotlin.math.sin

/**
 * Waving Cell Animation - High Res Interference Cells.
 * Recreates the original cellular wrapping pattern using a high-res plasma field.
 */
class WavingCellAnimation : Animation {

    private var _palette: Palette = Palettes.get("Heat") ?: Palettes.get("Rainbow") ?: Palettes.getDefault()
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // State
    // Render to fixed resolution for consistent "Cell" density regardless of screen size
    // and for performance. 
    private val RENDER_W = 100
    private val RENDER_H = 100
    
    private var buffer: Bitmap? = null
    private val paint = Paint().apply { 
        isAntiAlias = true 
        isFilterBitmap = true // Bilinear scaling for smooth "glowing" cells
    }
    private val destRect = Rect()
    
    // Params
    private var paramSpeed: Int = 128
    
    private var timeSeconds: Double = 0.0

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // 1. Init Buffer
        if (buffer == null) {
            buffer = Bitmap.createBitmap(RENDER_W, RENDER_H, Bitmap.Config.ARGB_8888)
        }
        val buf = buffer ?: return
        
        // 2. Update
        val speed = 0.05 + (paramSpeed / 255.0) * 0.15
        timeSeconds += speed
        val t = timeSeconds
        
        // 3. Render Interference Field (The "Cells")
        // Math based on original WavingCell:
        // sin8(x + sin8(y + t)) ...
        
        val pixels = IntArray(RENDER_W * RENDER_H)
        
        // Frequencies adjusted to match original "Magnified" look
        // Original: ~0.6 cycles per screen. 
        // We want ~0.6 * 2PI = 3.7 radians over 100 pixels -> ~0.04
        val freqY = 0.05
        val freqX = 0.05
        
        for (y in 0 until RENDER_H) {
            // inner = sin(y + t)
            val inner = sin(y * freqY + t) * 2.0 // Amplitude determines warping amount
            
            // vertical term for variation
            val vert = cos(y * freqY * 2.0)
            
            for (x in 0 until RENDER_W) {
                // wave = sin(x + inner)
                val wave = sin(x * freqX + inner)
                
                // Color Index calculation
                // Combine wave value + vertical variation + time for cycling
                // Map [-2..2] range to [0..255]
                
                val rawIndex = (wave + vert) * 128 + (t * 20)
                val index = rawIndex.toInt() % 256
                
                // Lookup color
                pixels[y * RENDER_W + x] = _palette.getInterpolatedInt(index)
            }
        }
        
        buf.setPixels(pixels, 0, RENDER_W, 0, 0, RENDER_W, RENDER_H)
        
        // 4. Draw Scaled
        destRect.set(0, 0, width.toInt(), height.toInt())
        canvas.drawBitmap(buf, null, destRect, paint)
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
    }
}
