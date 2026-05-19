package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.math.cos
import kotlin.math.sin

class PsGeqNovaAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private var fftMeter: FftMeter? = null
    
    // Cooldown to prevent continuous unbroken explosions
    private var lastNovaTime = 0L

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // Zero gravity, high friction so they explode out then rapidly slow down in the void
        physics = PhysicsEnvironment(width, height, gravityX = 0f, gravityY = 0f, restitution = 0.5f, friction = 0.85f)
        lastUpdate = System.currentTimeMillis()
        fftMeter = FftMeter(bands = 256)
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // Use normalized AGC bands to trigger only on loud dynamic drops
        val bands = fftMeter?.getNormalizedBands()
        val amplitude = bands?.average()?.toInt() ?: 0
        
        // Trigger a Nova if amplitude is extremely high and cooldown has passed
        if (amplitude > 160 && now - lastNovaTime > 200) {
            lastNovaTime = now
            
            // Explode a massive dense ring of particles
            val numParticles = 30 + (amplitude / 255f * 30).toInt()
            val timeOffset = (System.currentTimeMillis() / 15) % 255
            
            for (i in 0 until numParticles) {
                val angle = (i.toFloat() / numParticles) * Math.PI.toFloat() * 2f
                val speed = 30f + (amplitude / 255f) * 60f
                val vx = (cos(angle) * speed).toFloat()
                val vy = (sin(angle) * speed).toFloat()
                
                val colorIndex = (timeOffset + i * 5).toInt() % 255
                val color = getColorFromPalette(colorIndex)
                
                val p = Particle2D(width / 2f, height / 2f, vx, vy, color = color, lifetime = 1f, decayRate = 0.08f)
                p.bounceX = true
                p.bounceY = true
                p.size = 3
                particles.add(p)
            }
        }
        
        physics.updateParticles(particles, dt)
        particles.removeAll { !it.active }
        
        fadeToBlackBy(45)
        
        for (p in particles) {
            val dimColor = fadeColor(p.color, ((1f - p.lifetime) * 120).toInt().coerceIn(0, 255))
            drawBlob(p.x, p.y, dimColor, p.size)
        }
        
        return true
    }
    
    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
    }
}
