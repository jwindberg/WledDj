package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.marsraver.wleddj.engine.Animation
import kotlin.math.sin
import kotlin.random.Random

class FireworksAnimation : Animation {
    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var color: Int,
        var alpha: Float,
        var life: Float
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint().apply { isAntiAlias = true }
    private var timeUntilNextFirework = 0f
    
    // Audio
    private val loudnessMeter = com.marsraver.wleddj.engine.audio.LoudnessMeter()

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val loudness = loudnessMeter.getNormalizedLoudness()
        // Normalized 0-1024
        
        // Spawn Logic
        timeUntilNextFirework -= 1f 
        
        // Audio Reactivity:
        // Increase spawn chance if loud
        val spawnThreshold = if (loudness > 600) 5f else 60f // Frames
        
        if (timeUntilNextFirework <= 0) {
            spawnFirework(width, height)
            
            // Random interval, modulated by loudness
            // Loud = fast interval (5-20 frames)
            // Quiet = slow interval (30-90 frames)
            val baseInterval = if (loudness > 500) 10f else 60f
            val variance = if (loudness > 500) 10f else 60f
            timeUntilNextFirework = Random.nextFloat() * variance + baseInterval
        }
        // Force spawn on big impact?
        if (loudnessMeter.getPeakLoudness() > 900 && timeUntilNextFirework > 5) {
             // Beat detection-ish (very crude)
             // Not implemented fully to avoid spamming
        }

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.1f // Gravity
            p.life -= 0.02f
            p.alpha = p.life.coerceIn(0f, 1f)

            if (p.life <= 0 || p.x < 0 || p.x > width || p.y > height) {
                iterator.remove()
            }
        }

        particles.forEach { p ->
            paint.color = p.color
            paint.alpha = (p.alpha * 255).toInt()
            canvas.drawCircle(p.x, p.y, 3f, paint)
        }
    }

    private fun spawnFirework(w: Float, h: Float) {
        // ... spawn logic
        val cx = Random.nextFloat() * w
        val cy = Random.nextFloat() * h * 0.5f 
        val baseColor = Color.HSVToColor(floatArrayOf(Random.nextFloat() * 360f, 1f, 1f))
        
        for (i in 0 until 50) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = Random.nextFloat() * 5f + 2f
            particles.add(
                Particle(
                    x = cx, y = cy,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    color = baseColor,
                    alpha = 1f,
                    life = 1f
                )
            )
        }
    }
    
    override fun destroy() {
        loudnessMeter.stop()
    }
}

class AuroraBorealisAnimation : Animation {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f 
        isAntiAlias = true
    }
    private var offset = 0f


    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Standard speed
        offset += 0.05f
        
        // Draw 3 layers of waves
        drawWave(canvas, width, height, color = Color.GREEN, speed = 1f, yParams = Triple(0.3f, 50f, 0f))
        drawWave(canvas, width, height, color = Color.CYAN, speed = 0.7f, yParams = Triple(0.4f, 60f, 2f))
        drawWave(canvas, width, height, color = Color.MAGENTA, speed = 1.3f, yParams = Triple(0.5f, 40f, 4f))
    }

    private fun drawWave(canvas: Canvas, w: Float, h: Float, color: Int, speed: Float, yParams: Triple<Float, Float, Float>) {
        val (baseYFactor, amp, phaseShift) = yParams
        val path = Path()
        
        paint.color = color
        paint.alpha = 100 
        paint.strokeWidth = 30f 
        
        val points = 50
        val step = w / points
        
        path.moveTo(0f, h * baseYFactor + sin(offset * speed + phaseShift) * amp)
        
        for (i in 1..points) {
            val x = i * step
            val angle = offset * speed + (x / w) * 10f + phaseShift
            val y = h * baseYFactor + sin(angle) * amp + sin(angle * 0.5f) * amp * 0.5f
            path.lineTo(x, y)
        }
        
        canvas.drawPath(path, paint)
    }
    
    override fun destroy() {
        // No cleanup needed
    }
}

class FlashlightAnimation : Animation {
    private var targetX = -1f
    private var targetY = -1f
    
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Init center if not set
        if (targetX < 0) {
            targetX = width / 2f
            targetY = height / 2f
        }
        
        // Fixed radius (Ball size approx = 30f)
        val radius = 30f 
        
        // Yellow Glowing Spot
        // RadialGradient: Center (Yellow), Edge (Transparent)
        val shader = android.graphics.RadialGradient(
            targetX, targetY,
            radius,
            intArrayOf(Color.YELLOW, Color.TRANSPARENT),
            floatArrayOf(0.2f, 1.0f),
            android.graphics.Shader.TileMode.CLAMP
        )
        
        paint.shader = shader
        
        // Draw full rect to allow spill
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    override fun onTouch(x: Float, y: Float): Boolean {
        targetX = x
        targetY = y
        return true
    }
}
