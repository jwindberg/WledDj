package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.random.Random

class PsVolcanoAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        physics = PhysicsEnvironment(width, height, gravityY = 20f, restitution = 0.6f, friction = 0.8f)
        lastUpdate = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // Erupt
        val eruptionRate = (paramIntensity / 255f) * 2f
        val numToErupt = if (Random.nextFloat() < eruptionRate) 1 + Random.nextInt(3) else 0
        
        if (particles.size < 150) {
            for (i in 0 until numToErupt) {
                erupt()
            }
        }
        
        // Update physics
        physics.updateParticles(particles, dt)
        
        // Remove dead particles
        particles.removeAll { !it.active }
        
        // Render
        fadeToBlackBy(35)
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
    
    private fun erupt() {
        val x = width / 2f + (Random.nextFloat() - 0.5f) * 4f
        val y = height - 1f
        val vx = (Random.nextFloat() - 0.5f) * 20f // Spread
        val vy = -35f - Random.nextFloat() * 20f // Erupt upward fast
        
        val color = getColorFromPalette(Random.nextInt(255))
        
        val particle = Particle2D(x, y, vx, vy, color = color, lifetime = 1f, decayRate = 0.02f + Random.nextFloat() * 0.03f)
        particle.bounceX = true
        particle.bounceY = false
        particle.groundCollision = true
        particles.add(particle)
    }
}
