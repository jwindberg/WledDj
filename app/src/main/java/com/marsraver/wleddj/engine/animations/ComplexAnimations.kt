package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.marsraver.wleddj.engine.Animation
import kotlin.math.sin
import kotlin.random.Random

class FireworksAnimation : Animation {
    private enum class Type { ROCKET, SPARK }

    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var color: Int,
        var alpha: Float,
        var life: Float,
        val type: Type
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint().apply { isAntiAlias = true }
    private var timeUntilNextFirework = 0f

    // Physics Constants
    private val GRAVITY = 0.15f

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Spawn Logic
        timeUntilNextFirework -= 1f 
        
        if (timeUntilNextFirework <= 0) {
            spawnRocket(width, height)
            
            // Random interval: 0.5s to 2.0s (30 to 120 frames at 60fps)
            val baseInterval = 30f
            val variance = 90f
            timeUntilNextFirework = Random.nextFloat() * variance + baseInterval
        }
        
        val newParticles = mutableListOf<Particle>()
        val iterator = particles.iterator()
        
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += GRAVITY 

            if (p.type == Type.ROCKET) {
                // Rocket Logic
                // Trailing sparks - Reduced amount
                if (Random.nextFloat() > 0.6f) { // 40% chance per frame (was 2x 70%)
                     newParticles.add(Particle(p.x, p.y, 
                         (Random.nextFloat()-0.5f) * 2f, (Random.nextFloat()-0.5f) * 2f, 
                         Color.rgb(180, 180, 180), 0.5f, 0.3f, Type.SPARK))
                }

                // Explode at Apex 
                if (p.vy >= 0) { 
                    explodeRocket(p, newParticles)
                    iterator.remove()
                    continue
                }
                 
            } else {
                // Spark Logic
                p.life -= 0.012f 
                p.alpha = p.life.coerceIn(0f, 1f)
                p.vx *= 0.96f // Air resistance
                p.vy *= 0.96f
            }

            if (p.life <= 0 || p.y > height + 200) {
                iterator.remove()
            }
        }
        
        particles.addAll(newParticles)

        particles.forEach { p ->
            paint.color = p.color
            paint.alpha = (p.alpha * 255).toInt()
            
            // Tuned Sizes
            // Rocket: 12f (was 25f)
            // Spark: 8f (was 12f)
            val radius = if (p.type == Type.ROCKET) 12f else 8f
            
            canvas.drawCircle(p.x, p.y, radius, paint)
        }
    }

    private fun spawnRocket(w: Float, h: Float) {
        val x = Random.nextFloat() * w * 0.6f + w * 0.2f 
        val y = h
        
        // Aim for 50% to 80% screen height
        val targetH = h * (0.5f + Random.nextFloat() * 0.3f)
        val launchV = -kotlin.math.sqrt(2 * GRAVITY * targetH)
        
        val vx = (Random.nextFloat() - 0.5f) * 3f 
        
        particles.add(
            Particle(
                x, y, vx, launchV,
                Color.WHITE, 1f, 100f, Type.ROCKET
            )
        )
    }

    private fun explodeRocket(rocket: Particle, outList: MutableList<Particle>) {
        val isMultiColor = Random.nextFloat() < 0.3f // 30% chance of confetti
        val baseHue = Random.nextFloat() * 360f
        val baseColor = Color.HSVToColor(floatArrayOf(baseHue, 1f, 1f))
        
        for (i in 0 until 100) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = Random.nextFloat() * 7f + 2f 
            
            val pColor = if (isMultiColor) {
                 Color.HSVToColor(floatArrayOf(Random.nextFloat() * 360f, 1f, 1f))
            } else {
                 if (Random.nextBoolean()) baseColor else Color.WHITE
            }
            
            outList.add(
                Particle(
                    x = rocket.x, y = rocket.y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    color = pColor,
                    alpha = 1f,
                    life = Random.nextFloat() * 0.8f + 0.5f, 
                    type = Type.SPARK
                )
            )
        }
    }
    
    override fun destroy() {
        // No cleanup needed
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
    
    // Default to Yellow (classic flashlight)
    private var _primaryColor: Int = Color.YELLOW
    
    override var primaryColor: Int
        get() = _primaryColor
        set(value) { _primaryColor = value }

    override fun supportsPrimaryColor(): Boolean = true

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
        
        // Glowing Spot using Primary Color
        // RadialGradient: Center (Color), Edge (Transparent)
        val shader = android.graphics.RadialGradient(
            targetX, targetY,
            radius,
            intArrayOf(_primaryColor, Color.TRANSPARENT),
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
