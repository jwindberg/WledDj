package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.random.Random

class PsBallpitAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private val maxParticles = 50

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        physics = PhysicsEnvironment(width, height, gravityY = 25f, restitution = 0.85f, friction = 0.5f)
        lastUpdate = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false // Target ~60fps
        lastUpdate = now
        
        // Speed scales time logic
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        val targetParticles = (paramSpeed / 255f * maxParticles).toInt().coerceAtLeast(5)
        
        // Spawn new particles
        if (particles.size < targetParticles && Random.nextFloat() < 0.3f) {
            spawnParticle()
        }
        
        // Update physics
        physics.updateParticles(particles, dt)
        
        // Remove dead particles
        particles.removeAll { !it.active }
        
        // Render
        fadeToBlackBy(40) // Trails
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
    
    private fun spawnParticle() {
        val x = Random.nextFloat() * width
        val y = 0f
        val vx = (Random.nextFloat() - 0.5f) * 15f
        val vy = Random.nextFloat() * 5f
        
        val color = getColorFromPalette(Random.nextInt(255))
        
        particles.add(Particle2D(x, y, vx, vy, color = color, lifetime = 1f, decayRate = 0.05f))
    }
}
