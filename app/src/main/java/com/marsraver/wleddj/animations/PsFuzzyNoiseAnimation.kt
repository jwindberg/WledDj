package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PsFuzzyNoiseAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private var timeTracker = 0f

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // High friction, no gravity. Particles are entirely moved by the noise field
        physics = PhysicsEnvironment(width, height, gravityX = 0f, gravityY = 0f, restitution = 1f, friction = 0.7f)
        lastUpdate = System.currentTimeMillis()
        
        for (i in 0 until 60) {
            spawnParticle()
        }
    }
    
    private fun spawnParticle() {
        val color = getColorFromPalette(Random.nextInt(255))
        val p = Particle2D(Random.nextFloat() * width, Random.nextFloat() * height, 0f, 0f, color = color, lifetime = 1f, decayRate = 0.02f)
        p.bounceX = false // Let them wrap around or die
        p.bounceY = false
        p.size = 2
        particles.add(p)
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        timeTracker += dt * 0.5f
        
        // Maintain particle count
        while (particles.size < 60) {
            spawnParticle()
        }
        
        // Apply noise field forces
        val noiseScale = 0.15f
        val forceMagnitude = 200f
        
        for (p in particles) {
            // Pseudo-random noise field using overlapping sine waves
            // This creates flowing, organic, insect-like swarming movements
            val nx = p.x * noiseScale
            val ny = p.y * noiseScale
            
            val angle = (sin(nx + timeTracker) + cos(ny - timeTracker) + sin(nx * 0.5f + ny * 0.5f + timeTracker * 2f)) * Math.PI.toFloat()
            
            p.vx += (cos(angle) * forceMagnitude * dt).toFloat()
            p.vy += (sin(angle) * forceMagnitude * dt).toFloat()
            
            // Wrap around screen instead of dying
            if (p.x < 0) p.x += width
            if (p.x >= width) p.x -= width
            if (p.y < 0) p.y += height
            if (p.y >= height) p.y -= height
        }
        
        physics.updateParticles(particles, dt)
        particles.removeAll { !it.active }
        
        fadeToBlackBy(70)
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
}
