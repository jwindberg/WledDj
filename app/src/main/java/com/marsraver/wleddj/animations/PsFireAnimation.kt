package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.random.Random

class PsFireAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // Negative gravity acts as buoyancy (heat rising)
        physics = PhysicsEnvironment(width, height, gravityY = -15f, restitution = 0.0f, friction = 0.9f)
        lastUpdate = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // Spawn fire particles at the bottom
        val spawnRate = (paramIntensity / 255f) * 5f
        val numToSpawn = if (Random.nextFloat() < spawnRate) 1 + Random.nextInt(4) else 0
        
        if (particles.size < 200) {
            for (i in 0 until numToSpawn) {
                spawnFlame()
            }
        }
        
        // Apply turbulence
        val wind = 0f // Can be tied to a parameter later
        for (p in particles) {
            val turbulence = (Random.nextFloat() - 0.5f) * 15f
            p.vx += (wind + turbulence) * dt
        }
        
        // Update physics
        physics.updateParticles(particles, dt)
        
        // Remove dead particles or those that reach the top
        particles.removeAll { !it.active || it.y < 0f }
        
        // Render
        fadeToBlackBy(45) // Fast fade for fire trails
        
        for (p in particles) {
            // Dim based on lifetime to simulate cooling
            val color = fadeColor(p.color, ((1f - p.lifetime) * 150).toInt().coerceIn(0, 255))
            drawBlob(p.x, p.y, color, p.size)
        }
        
        return true
    }
    
    private fun spawnFlame() {
        val x = Random.nextFloat() * width
        val y = height - 1f
        val vx = (Random.nextFloat() - 0.5f) * 2f
        val vy = -5f - Random.nextFloat() * 10f
        
        val color = getColorFromPalette(Random.nextInt(255))
        
        val particle = Particle2D(x, y, vx, vy, color = color, lifetime = 1f, decayRate = 0.5f + Random.nextFloat() * 0.5f)
        particle.bounceX = false
        particle.bounceY = false
        particle.groundCollision = false
        particle.size = 2 // Fire particles are slightly smaller
        particles.add(particle)
    }
}
