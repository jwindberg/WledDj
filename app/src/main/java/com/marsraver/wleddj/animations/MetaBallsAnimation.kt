package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * MetaBalls Animation - High Res Lava Lamp.
 * Smooth metabolic field rendered as a shader.
 */
class MetaBallsAnimation : Animation {

    private var _palette: Palette = Palette.RAINBOW
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true
    
    // Speed Support
    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        paramSpeed = (speed * 255f).toInt().coerceIn(0, 255)
    }
    override fun getSpeed(): Float = paramSpeed / 255f

    // State
    private val RENDER_W = 80
    private val RENDER_H = 80
    
    private var buffer: Bitmap? = null
    private val paint = Paint().apply { 
        isAntiAlias = true 
        isFilterBitmap = true // Bilinear scaling for smooth goo
    }
    private val destRect = Rect()

    // Balls
    private data class Ball(
        var x: Float, 
        var y: Float, 
        var radius: Float,
        var speedX: Float,
        var speedY: Float
    )
    private val balls = mutableListOf<Ball>()

    // Params
    private var paramSpeed: Int = 128
    private var timeSeconds: Double = 0.0

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // 1. Init
        if (buffer == null) {
            buffer = Bitmap.createBitmap(RENDER_W, RENDER_H, Bitmap.Config.ARGB_8888)
            // Create balls
            balls.clear()
            repeat(5) {
                balls.add(Ball(
                    x = Math.random().toFloat() * RENDER_W,
                    y = Math.random().toFloat() * RENDER_H,
                    radius = 8f + Math.random().toFloat() * 10f, // Influence radius
                    speedX = 0f, 
                    speedY = 0f
                ))
            }
        }
        val buf = buffer ?: return
        
        // 2. Update Balls (Lissajous/Sine motion for smooth looping)
        val speed = 0.02 + (paramSpeed / 255.0) * 0.05
        timeSeconds += speed
        val t = timeSeconds
        
        balls.forEachIndexed { i, ball ->
            // Lissajous-ish motion
            val f1 = 0.5 + (i * 0.1)
            val f2 = 0.7 + (i * 0.13)
            
            // Map -1..1 to 0..W
            val nx = sin(t * f1 + i) 
            val ny = cos(t * f2 + i * 2)
            
            ball.x = (RENDER_W / 2f) + nx.toFloat() * (RENDER_W * 0.4f)
            ball.y = (RENDER_H / 2f) + ny.toFloat() * (RENDER_H * 0.4f)
            
            // Breathing radius?
            ball.radius = 10f + sin(t * 2.0 + i).toFloat() * 3f
        }
        
        // 3. Render Field
        val pixels = IntArray(RENDER_W * RENDER_H)
        
        for (y in 0 until RENDER_H) {
            for (x in 0 until RENDER_W) {
                var sum = 0.0f
                
                // Metabolic potential formula: Sum(Radius / Distance)
                // Or Standard: Sum(Radius^2 / Distance^2)
                // Let's use R / D for softer falloff, or R^2 / D^2 for sharper blobs
                
                for (b in balls) {
                    val dx = x - b.x
                    val dy = y - b.y
                    // Distance sq
                    val d2 = dx*dx + dy*dy
                    // Avoid div by zero
                    if (d2 < 1.0f) {
                        sum += 100f // clamped High value
                    } else {
                        // R^2 / D^2
                        sum += (b.radius * b.radius) / d2
                    }
                }
                
                // Sum dictates the "heat" at this pixel.
                // Thresholding creates the blob edge.
                
                // We want a smooth gradient though.
                // Map sum to Palette Index.
                // sum typically 0..5+ near centers.
                // Let's map 0..2.0 -> 0..255
                
                val index = (sum * 80).toInt().coerceIn(0, 255)
                
                // "Lava Lamp" look:
                // Background is usually dark (index 0). 
                // Blobs are bright.
                
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
