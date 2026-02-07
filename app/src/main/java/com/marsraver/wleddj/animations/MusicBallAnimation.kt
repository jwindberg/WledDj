package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import kotlin.random.Random

class MusicBallAnimation : BasePixelAnimation() {

    override fun supportsPrimaryColor(): Boolean = false
    override fun supportsPalette(): Boolean = true
    
    // BasePixelAnimation handles palette access via getColorFromPalette
    
    // We don't need these override properties anymore as BasePixelAnimation has them
    
    private data class Ball(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val color: Int,
        var age: Int = 0,
        var alive: Boolean = true
    ) {
        fun update(height: Float) {
            age += 3
            vy += 0.1f // Gravity
            x += vx
            y += vy
            
            if (y > height || age >= 255) {
                alive = false
            }
        }
    }

    private val balls = mutableListOf<Ball>()
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val bgPaint = Paint().apply {
        color = Color.BLACK
        alpha = 45 // Fade trail effect
    }
    
    // Sequential color index
    private var paletteIndex = 0
    
    override fun onInit() {
        // No specific init needed, clean slate
        balls.clear()
        paletteIndex = 0
    }
    
    override fun update(now: Long): Boolean {
         // This is called by BasePixelAnimation's draw (if used) or we call it manually
         return true
    }

    private val loudnessMeter = LoudnessMeter()
    
    // Params
    // paramSpeed is inherited from BasePixelAnimation, DO NOT re-declare it.
    
    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        paramSpeed = (speed * 255f).toInt().coerceIn(0, 255)
    }
    override fun getSpeed(): Float = paramSpeed / 255f

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Fade previous frame
        canvas.drawRect(0f, 0f, width, height, bgPaint)
        
        // Audio Logic
        val normalizedLoudness = loudnessMeter.getNormalizedLoudness()
        // Threshold for spawn (tuned to be reactive)
        // 512 is mid-range. 
        val spawnThreshold = 300
        val isBeat = normalizedLoudness > spawnThreshold && Random.nextFloat() < 0.3f
        
        if (isBeat) { 
             val level = normalizedLoudness / 1024f
             val spawnCount = (level * 5).toInt().coerceAtLeast(1)
             
             repeat(spawnCount) {
                 // "Next" color for each ball
                 paletteIndex = (paletteIndex + 1) % 256
                 val col = getColorFromPalette(paletteIndex)
                 val bx = Random.nextFloat() * width
                 val by = Random.nextFloat() * height
                 
                 // Speed Boost
                 val speedFactor = 0.5f + (paramSpeed / 255f) * 1.5f
                 val mag = level * 70f * speedFactor
                 
                 val vx = (Random.nextFloat() - 0.5f) * 2f
                 val vy = (Random.nextFloat() - 0.5f) * 2f
                 val len = kotlin.math.sqrt(vx*vx + vy*vy)
                 val nvx = (vx / len) * mag * 0.1f
                 val nvy = (vy / len) * mag * 0.1f
                 
                 balls.add(Ball(
                     x = bx, y = by,
                     vx = nvx, vy = nvy,
                     color = col
                 ))
             }
        }
        
        // Update and Draw
        val iterator = balls.iterator()
        while (iterator.hasNext()) {
            val b = iterator.next()
            b.update(height)
            if (!b.alive) {
                iterator.remove()
            } else {
                paint.color = b.color
                paint.alpha = (255 - b.age).coerceIn(0, 255)
                canvas.drawCircle(b.x, b.y, 10f, paint) 
            }
        }
    }
}

