package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.BeatDetector
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class BlackHoleAnimation : Animation {

    private data class Matter(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val color: Int,
        var mass: Float
    ) {
        fun update(cx: Float, cy: Float, eventHorizon: Float) {
            val dx = cx - x
            val dy = cy - y
            val dist = hypot(dx, dy)
            
            if (dist < eventHorizon) {
                // Consumed! Respawn far away
                respawn(cx, cy, eventHorizon * 10f)
                return
            }
            
            // Gravity = G * M / r^2
            // Force direction
            val force = 500f / (dist * dist + 1f) // Softened gravity
            val ax = (dx / dist) * force
            val ay = (dy / dist) * force
            
            vx += ax
            vy += ay
            
            // Drag/Friction (Accretion disk viscosity)
            vx *= 0.98f
            vy *= 0.98f
            
            x += vx
            y += vy
        }
        
        fun respawn(cx: Float, cy: Float, range: Float) {
            val angle = Random.nextFloat() * 6.28f
            val d = range + Random.nextFloat() * range // Far out
            x = cx + cos(angle) * d
            y = cy + sin(angle) * d
            
            // Orbital Velocity: V = sqrt(GM/r)
            // Tangential direction
            val pAngle = angle + 1.57f // 90 deg
            val orbSpeed = kotlin.math.sqrt(500f / d) * 1.5f // Initial push
            
            vx = cos(pAngle) * orbSpeed
            vy = sin(pAngle) * orbSpeed
            
            mass = Random.nextFloat() * 2f + 1f
        }
    }

    private val particles = mutableListOf<Matter>()
    private var singularityX = 0f
    private var singularityY = 0f
    private var initialized = false
    
    // Paints
    private val starPaint = Paint().apply { style = Paint.Style.FILL }
    private val voidPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val horizonPaint = Paint().apply { 
        style = Paint.Style.STROKE 
        strokeWidth = 2f
        color = Color.parseColor("#440022") // Purple glow
    }

    private fun ensureParticles(w: Float, h: Float) {
        if (!initialized) {
            singularityX = w / 2f
            singularityY = h / 2f
            
            // Initial spawn
            repeat(200) {
                val m = Matter(0f, 0f, 0f, 0f, 0, 0f)
                // Random Accretion Colors: Orange, Red, White, Blue
                val hue = Random.nextFloat()
                val col = if (hue < 0.6f) {
                    Color.rgb(255, (Random.nextFloat()*100).toInt(), 0) // Red/Orange
                } else {
                    Color.rgb(200, 200, 255) // Blue/White hot
                }
                m.copy(color = col).also { mp ->
                    mp.respawn(singularityX, singularityY, minOf(w, h)/2f)
                    particles.add(mp)
                }
            }
            initialized = true
        }
        
        // Spawn more based on audio?
        if (BeatDetector.isRange(0, 5, 20)) {
            // Pulse: Maybe add a few
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        ensureParticles(width, height)
        
        // Draw Void (clears background implicitly if opaque, but we assume transparent context)
        // Actually, we want a fade trail?
        // Let's just draw fresh.
        canvas.drawColor(Color.argb(20, 0, 0, 0)) // Fade trails manually?
        // Actually canvas.drawColor overwrites. 
        // If we want trails, better to NOT clear and use a semi-transparent rect.
        canvas.drawRect(0f, 0f, width, height, Paint().apply { color = Color.BLACK; alpha = 30 })
        
        val horizon = 30f // Event horizon radius
        
        // Singularity Glow (Audio Reactive radius)
        val level = BeatDetector.getLevel()
        val glowRadius = horizon * (1f + level * 2f)
        
        val glowPaint = Paint().apply {
            shader = RadialGradient(
                singularityX, singularityY, glowRadius * 3f,
                intArrayOf(Color.parseColor("#FF4400"), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(singularityX, singularityY, glowRadius * 3f, glowPaint)

        // Draw Event Horizon (Void)
        canvas.drawCircle(singularityX, singularityY, horizon, voidPaint)
        
        // Update/Draw Particles
        particles.forEach { p ->
            p.update(singularityX, singularityY, horizon)
            
            starPaint.color = p.color
            // Stretch based on velocity (spaghettification visual)
            val speed = hypot(p.vx, p.vy)
            val angle = atan2(p.vy, p.vx)
            
            canvas.save()
            canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat(), p.x, p.y)
            canvas.drawOval(
                p.x - speed, p.y - p.mass, 
                p.x + speed, p.y + p.mass, 
                starPaint
            )
            canvas.restore()
        }
    }

    override fun onTouch(x: Float, y: Float): Boolean {
        // Move Singularity
        singularityX = x
        singularityY = y
        return true
    }
}
