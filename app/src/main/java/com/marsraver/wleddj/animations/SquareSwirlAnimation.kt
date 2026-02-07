package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.sin

/**
 * Square Swirl animation - Vector based.
 * Nested rotating squares moving in sine patterns.
 */
class SquareSwirlAnimation : Animation {

    private var _palette: Palette = Palette.RAINBOW
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // State
    private var buffer: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    private val paint = Paint().apply { 
        isAntiAlias = true 
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val fadePaint = Paint().apply { 
        color = Color.BLACK 
        alpha = 30 // Trails
    }
    private val clearRect = Rect()

    // Params
    private var paramSpeed: Int = 128

    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        paramSpeed = (speed * 255f).toInt().coerceIn(0, 255)
    }
    override fun getSpeed(): Float = paramSpeed / 255f
    
    // Logic State
    private var time: Float = 0f

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val w = width.toInt().coerceAtLeast(1)
        val h = height.toInt().coerceAtLeast(1)
        
        // 1. Buffer
        if (buffer == null || buffer?.width != w || buffer?.height != h) {
            buffer?.recycle()
            buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(buffer!!)
            bufferCanvas?.drawColor(Color.BLACK)
        }
        val bufCanvas = bufferCanvas ?: return
        
        // 2. Fade Trails
        clearRect.set(0, 0, w, h)
        bufCanvas.drawRect(clearRect, fadePaint)
        
        // 3. Update & Draw
        val speed = 0.05f + (paramSpeed / 255f) * 0.1f
        time += speed
        
        // Draw 3 Squares following sine paths
        val squareCount = 3
        
        for (i in 0 until squareCount) {
            // Colors
            val colorIndex = ((time * 20) + i * 85).toInt() % 256
            paint.color = _palette.getInterpolatedInt(colorIndex)
            
            // Movement Logic (beatsin style but float)
            // i=0: freq 91/256 roughly. 
            // Normalized freq? 
            
            val t = time
            val offset = i * 2.0f
            
            // X/Y based on sine waves with different frequencies
            // Map sine (-1..1) to screen rect (padding)
            val padding = minOf(width, height) * 0.2f
            val drawW = width - padding * 2
            val drawH = height - padding * 2
            
            // Different frequencies for chaotic Lissajous motion
            val freqX = 1.0f + i * 0.3f
            val freqY = 1.3f + i * 0.4f
            
            val cx = padding + drawW/2 + (drawW/2) * sin(t * freqX + offset)
            val cy = padding + drawH/2 + (drawH/2) * sin(t * freqY + offset * 1.5f)
            
            // Size
            val size = minOf(width, height) * 0.15f
            
            // Rotation
            val rot = t * 50f + i * 45f
            
            bufCanvas.save()
            bufCanvas.rotate(rot, cx, cy)
            // Draw Square centered at cx,cy
            bufCanvas.drawRect(cx - size/2, cy - size/2, cx + size/2, cy + size/2, paint)
            bufCanvas.restore()
        }
        
        // 4. Blit
        canvas.drawBitmap(buffer!!, 0f, 0f, null)
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
        bufferCanvas = null
    }
}
