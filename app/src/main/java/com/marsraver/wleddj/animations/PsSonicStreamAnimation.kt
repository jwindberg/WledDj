package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PsSonicStreamAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private var fftMeter: FftMeter? = null
    
    private var angle: Float = 0f

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // Zero gravity, particles shoot outwards
        physics = PhysicsEnvironment(width, height, gravityY = 0f, restitution = 1.0f, friction = 0.8f)
        lastUpdate = System.currentTimeMillis()
        fftMeter = FftMeter(bands = 256)
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        val bands = fftMeter?.getNormalizedBands()
        val amplitude = bands?.average()?.toInt() ?: 0
        
        // Rotating nozzle
        angle += dt * 5f
        
        // Spawn based on AGC amplitude (0-255). 100+ means it's a strong beat relative to the room
        if (amplitude > 100) {
            val numToSpawn = (amplitude / 40).coerceIn(1, 8)
            for (i in 0 until numToSpawn) {
                val spread = angle + (Random.nextFloat() - 0.5f) * 0.5f
                val speed = 10f + (amplitude / 255f) * 60f
                val vx = (cos(spread) * speed).toFloat()
                val vy = (sin(spread) * speed).toFloat()
                
                // Combine time and amplitude to cycle through the entire palette beautifully
                val timeOffset = (System.currentTimeMillis() / 15) % 255
                val colorIndex = (timeOffset + amplitude).toInt() % 255
                val color = getColorFromPalette(colorIndex)
                val p = Particle2D(width / 2f, height / 2f, vx, vy, color = color, lifetime = 1f, decayRate = 0.5f) // Faster decay
                p.bounceX = false // Don't bounce, fly out like a sprinkler
                p.bounceY = false
                p.size = 2
                particles.add(p)
            }
        }
        
        physics.updateParticles(particles, dt)
        
        // Remove dead particles or those that flew far off screen
        particles.removeAll { 
            !it.active || 
            it.x < -10f || it.x > width + 10f || 
            it.y < -10f || it.y > height + 10f 
        }
        
        fadeToBlackBy(80) // Faster fade for less background clutter
        
        for (p in particles) {
            val color = fadeColor(p.color, ((1f - p.lifetime) * 100).toInt().coerceIn(0, 255))
            drawBlob(p.x, p.y, color, p.size)
        }
        return true
    }
    
    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
    }
}
