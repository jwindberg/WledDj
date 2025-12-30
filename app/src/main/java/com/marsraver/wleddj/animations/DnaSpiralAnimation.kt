package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.sin
import kotlin.math.PI

/**
 * DNA Spiral Animation - Vector Helix.
 */
class DnaSpiralAnimation : Animation {

    private var _palette: Palette = Palette.RAINBOW
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // State
    private var buffer: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    private val paint = Paint().apply { isAntiAlias = true }
    private val fadePaint = Paint().apply { 
        color = Color.BLACK 
        alpha = 50 // Trails slightly
    }
    private val clearRect = Rect()

    // Params
    private var paramSpeed: Int = 128
    
    // Logic
    private var timeSeconds: Double = 0.0

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val w = width.toInt().coerceAtLeast(1)
        val h = height.toInt().coerceAtLeast(1)
        
        // 1. Buffer (Optional for this one, but good for trails if we want them?
        // Original didn't use trails much, just pure redraw or slight fade.
        // Let's use buffer for smooth clearing/trails.
        
        if (buffer == null || buffer?.width != w || buffer?.height != h) {
            buffer?.recycle()
            buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(buffer!!)
            bufferCanvas?.drawColor(Color.BLACK)
        }
        val bufCanvas = bufferCanvas ?: return
        
        // Fade
        clearRect.set(0, 0, w, h)
        // Strong fade to clear effectively, but leave slight blur
        fadePaint.alpha = 80
        bufCanvas.drawRect(clearRect, fadePaint)
        
        // 2. DNA Logic
        val speed = (paramSpeed / 255.0) * 0.05
        timeSeconds += speed
        
        val rowCount = 20 // Number of rungs
        val rowHeight = height / rowCount
        val center = width / 2f
        val amplitude = width * 0.4f
        
        for (i in 0 until rowCount) {
             val y = i * rowHeight + rowHeight/2
             
             // Phase
             val phase = i * 0.5 + timeSeconds * 2.0
             
             // Two strands
             val x1 = center + sin(phase) * amplitude
             val x2 = center + sin(phase + PI) * amplitude
             
             // Draw Rung
             paint.style = Paint.Style.STROKE
             paint.strokeWidth = 4f
             // Color based on height/index
             val colorIndex = ((i * 10) + (timeSeconds * 50)).toInt() % 256
             paint.color = _palette.getInterpolatedInt(colorIndex)
             paint.alpha = 150
             
             bufCanvas.drawLine(x1.toFloat(), y, x2.toFloat(), y, paint)
             
             // Draw Nucleotides (Ends)
             paint.style = Paint.Style.FILL
             paint.alpha = 255
             
             // Size oscillates for 3D effect?
             // Z-ordering simulation: if sin(phase) is positive -> front?
             val z1 = kotlin.math.cos(phase) // deriv of sin is cos
             val r1 = 8f + z1 * 3f
             
             val z2 = kotlin.math.cos(phase + PI)
             val r2 = 8f + z2 * 3f
             
             bufCanvas.drawCircle(x1.toFloat(), y, r1.toFloat(), paint)
             
             // Second color
             paint.color = _palette.getInterpolatedInt((colorIndex + 128) % 256)
             bufCanvas.drawCircle(x2.toFloat(), y, r2.toFloat(), paint)
        }
        
        // 3. Blit
        canvas.drawBitmap(buffer!!, 0f, 0f, null)
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
        bufferCanvas = null
    }
}
