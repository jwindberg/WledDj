package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.random.Random

class PsImpactAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private var fftMeter: FftMeter? = null
    
    private var shakeMagnitude = 0f

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // High gravity, bouncy particles
        physics = PhysicsEnvironment(width, height, gravityX = 0f, gravityY = 30f, restitution = 0.9f, friction = 0.99f)
        lastUpdate = System.currentTimeMillis()
        fftMeter = FftMeter(bands = 256)
        
        // Spawn 40 permanent bouncing particles
        for (i in 0 until 40) {
            val color = getColorFromPalette((i * 6) % 255)
            val p = Particle2D(Random.nextFloat() * width, Random.nextFloat() * height, 0f, 0f, color = color, lifetime = 1f, decayRate = 0f)
            p.bounceX = true
            p.groundCollision = true
            p.size = 3
            particles.add(p)
        }
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        val bands = fftMeter?.getNormalizedBands()
        val amplitude = bands?.average()?.toInt() ?: 0
        
        // An impact beat! Give everyone a massive velocity kick to simulate the floor dropping or shaking
        if (amplitude > 130) {
            shakeMagnitude = (amplitude / 255f) * 4f // Screen shake magnitude
            
            for (p in particles) {
                // "Kick" the particles up and sideways randomly
                p.vy -= (amplitude / 255f) * 40f
                p.vx += (Random.nextFloat() - 0.5f) * (amplitude / 255f) * 60f
                
                // Color flash on impact
                p.color = getColorFromPalette(Random.nextInt(255))
            }
        }
        
        // Decay shake smoothly
        shakeMagnitude *= 0.8f
        
        physics.updateParticles(particles, dt)
        
        fadeToBlackBy(80)
        
        // Calculate random screen shake offset
        val shakeX = if (shakeMagnitude > 0.5f) (Random.nextFloat() - 0.5f) * shakeMagnitude else 0f
        val shakeY = if (shakeMagnitude > 0.5f) (Random.nextFloat() - 0.5f) * shakeMagnitude else 0f
        
        for (p in particles) {
            drawBlob(p.x + shakeX, p.y + shakeY, p.color, p.size)
        }
        
        return true
    }
    
    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
    }
}
