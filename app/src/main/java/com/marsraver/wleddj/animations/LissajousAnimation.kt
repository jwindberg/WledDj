package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import java.util.ArrayDeque
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

/**
 * Lissajous Animation - Vector Curve Tracer.
 * Draws smooth, rotating Lissajous figures.
 */
class LissajousAnimation : Animation {

    private var _palette: Palette = Palette.fromName("Rainbow") ?: Palette.DEFAULT
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // State
    private val paint = Paint().apply { 
        isAntiAlias = true 
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()

    // Params
    private var paramSpeed: Int = 128
    
    private var timeSeconds: Double = 0.0

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Clear
        canvas.drawColor(Color.BLACK)
        
        // Update Time
        val speed = 0.05 + (paramSpeed / 255.0) * 0.2
        timeSeconds += speed
        
        // Lissajous Parameters
        // We act like a 3D rotating structure
        val t = timeSeconds
        
        // Draw the curve
        path.reset()
        
        val segments = 200 // Resolution of curve
        val padding = minOf(width, height) * 0.1f
        val w = width - padding * 2
        val h = height - padding * 2
        val cx = width / 2f
        val cy = height / 2f
        
        // Frequencies change slowly for animation
        val freqX = 3.0 + sin(t * 0.1)
        val freqY = 2.0 + cos(t * 0.15)
        
        // Phase shift simulates rotation
        val phaseX = t * 1.0
        val phaseY = t * 1.5
        
        // Draw multiple overlapping curves or one long one?
        // One solid gradient curve is nice.
        
        // To do gradient along path, we'd need complex shader or segment drawing.
        // Let's do segment drawing for color cycling along the curve.
        
        var firstX = 0f
        var firstY = 0f
        
        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * 2 * PI
            
            // Lissajous math
            val vx = sin(angle * freqX + phaseX) // -1..1
            val vy = sin(angle * freqY + phaseY) // -1..1
            
            val px = cx + vx.toFloat() * (w / 2f)
            val py = cy + vy.toFloat() * (h / 2f)
            
            if (i == 0) {
                path.moveTo(px, py)
                firstX = px
                firstY = py
            } else {
                path.lineTo(px, py)
            }
        }
        // path.close() // Don't close, it might not meet if modulating?
        // Actually lissajous with rational freq ratios are closed. Our freq is varying so it might be open.
        
        // Color
        val colorIndex = (timeSeconds * 50).toInt() % 256
        paint.color = _palette.getInterpolatedInt(colorIndex)
        
        // Draw Glo?
        paint.strokeWidth = 8f
        paint.alpha = 100
        canvas.drawPath(path, paint)
        
        paint.strokeWidth = 3f
        paint.alpha = 255
        paint.color = Color.WHITE // Core
        canvas.drawPath(path, paint)
    }
}
