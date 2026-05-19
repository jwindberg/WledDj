package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PsSprayAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private var fftMeter: FftMeter? = null
    
    private var timeTracker = 0f

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // High gravity so the spray arcs over and falls back down
        physics = PhysicsEnvironment(width, height, gravityX = 0f, gravityY = 25f, restitution = 0.5f, friction = 0.98f)
        lastUpdate = System.currentTimeMillis()
        fftMeter = FftMeter(bands = 256)
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        timeTracker += dt * 3f
        
        val bands = fftMeter?.getNormalizedBands()
        val amplitude = bands?.average()?.toInt() ?: 0
        
        // Sweeping hose logic (sweeps from -PI/4 to +PI/4 around straight up)
        // Straight up is -PI/2 in screen coordinates
        val baseAngle = -Math.PI.toFloat() / 2f
        val sweepAngle = baseAngle + sin(timeTracker) * (Math.PI.toFloat() / 3f)
        
        // Spray when there is moderate audio
        if (amplitude > 80) {
            val numToSpawn = (amplitude / 50).coerceIn(1, 5)
            val timeOffset = (System.currentTimeMillis() / 20) % 255
            
            for (i in 0 until numToSpawn) {
                // Add slight jitter to the spray nozzle angle
                val sprayAngle = sweepAngle + (Random.nextFloat() - 0.5f) * 0.2f
                
                // Spray speed based on volume
                val speed = 30f + (amplitude / 255f) * 50f
                val vx = (cos(sprayAngle) * speed).toFloat()
                val vy = (sin(sprayAngle) * speed).toFloat()
                
                val colorIndex = (timeOffset + i * 10).toInt() % 255
                val color = getColorFromPalette(colorIndex)
                
                val p = Particle2D(width / 2f, height - 1f, vx, vy, color = color, lifetime = 1f, decayRate = 0.04f)
                p.bounceX = true
                p.groundCollision = false // Let them fall off the bottom of the screen
                p.size = 3
                particles.add(p)
            }
        }
        
        physics.updateParticles(particles, dt)
        
        // Remove dead particles and those that fall off the bottom
        particles.removeAll { !it.active || it.y > height + 5f }
        
        fadeToBlackBy(50)
        
        for (p in particles) {
            val dimColor = fadeColor(p.color, ((1f - p.lifetime) * 100).toInt().coerceIn(0, 255))
            drawBlob(p.x, p.y, dimColor, p.size)
        }
        
        return true
    }
    
    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
    }
}
