package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.BeatDetector
import kotlin.random.Random

class MusicBallAnimation : Animation {

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

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Fade previous frame
        canvas.drawRect(0f, 0f, width, height, bgPaint)
        
        // Spawn Balls on Beat
        // Legacy: beat.isRange(5, 15, 2)
        // beat.isRange checks energy in bands 5-15 (out of ?).
        // Our BeatDetector uses 16 bands. 5-15 covers Mid to High.
        // Threshold "2" in Minim is low? High?
        // Let's assume average energy > 10 (out of 255) to start spawning.
        // Since we don't have real "beat" detection (onset), this is just "Is there Sound?"
        // We'll use a threshold of 20 for noise floor.
        
        if (BeatDetector.isRange(0, 5, 20)) { // Changed to 0-5 (Bass/Low Mid) for better effect
             val level = BeatDetector.getLevel()
             val spawnCount = (level * 20).toInt().coerceAtLeast(1)
             
             // Spawn color
             // random(255) for RGB
             val col = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
             
             repeat(spawnCount) {
                 val bx = Random.nextFloat() * width
                 val by = Random.nextFloat() * height
                 val mag = level * 70f // Speed magnitude
                 
                 // Random speed normalized * mag
                 // Simply: random vector
                 val vx = (Random.nextFloat() - 0.5f) * 2f
                 val vy = (Random.nextFloat() - 0.5f) * 2f
                 // Normalize approx
                 val len = kotlin.math.sqrt(vx*vx + vy*vy)
                 val nvx = (vx / len) * mag * 0.1f // Scale down a bit? 
                 // Legacy: speed.mult(mag); audioInput.mix.get(0) * 70? 
                 // mix.get(0) is sample -1..1. level is 0..1.
                 // So speed is roughly 0..70 pixels/frame?? That's fast.
                 // We'll stick to reasonable speed.
                 
                 balls.add(Ball(
                     x = bx, y = by,
                     vx = nvx, vy = vy / len * mag * 0.1f, // nvy
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
                canvas.drawCircle(b.x, b.y, 10f, paint) // Ellipse 5,5 -> Radius 2.5? 10f is visible.
            }
        }
    }
}
