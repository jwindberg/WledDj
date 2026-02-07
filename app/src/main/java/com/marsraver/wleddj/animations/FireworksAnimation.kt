package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
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
    private val loudnessMeter = com.marsraver.wleddj.engine.audio.LoudnessMeter()
    private var lastVolume = 0f
    private var launchCooldown = 0

    // Physics Constants
    private val GRAVITY = 0.15f

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Audio Logic
        val volume = loudnessMeter.getNormalizedLoudness() / 1024f
        // Tuned Sensitivity: 0.1 threshold, 10% jump
        val beatDetected = volume > 0.1f && volume > lastVolume * 1.1f
        
        if (launchCooldown > 0) launchCooldown--
        
        // High Energy Backup: Constant loud volume (> 0.5) triggers random launches
        val highEnergy = volume > 0.5f && Random.nextFloat() < 0.1f
        
        if (((beatDetected || highEnergy) && launchCooldown <= 0) || Random.nextFloat() < 0.01f) {
            spawnRocket(width, height)
            launchCooldown = if (beatDetected) 8 else 20
        }
        lastVolume = volume
        
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
            
            // Tuned Sizes (Halved)
            // Rocket: 6f (was 12f)
            // Spark: 4f (was 8f)
            val radius = if (p.type == Type.ROCKET) 6f else 4f
            
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
        // Pick a random palette for this explosion
        val randomPalette = com.marsraver.wleddj.engine.color.Palette.entries.random()
        
        for (i in 0 until 100) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = Random.nextFloat() * 7f + 2f 
            
            // Pick a color from the chosen palette
            // We use a random index (0-255) to sample the palette gradient/colors
            val colorIndex = Random.nextInt(0, 256)
            val pColor = randomPalette.getInterpolatedInt(colorIndex)
            
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
        loudnessMeter.stop()
    }
}
