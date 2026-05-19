package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PsDripDropAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        physics = PhysicsEnvironment(width, height, gravityY = 20f, restitution = 0.4f, friction = 0.9f)
        lastUpdate = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // Drip from top
        val dripRate = (paramIntensity / 255f) * 0.5f
        if (Random.nextFloat() < dripRate && particles.count { it.type == 0 } < 5) {
            drip()
        }
        
        // Update physics
        physics.updateParticles(particles, dt)
        
        // Process splashes
        val splashes = mutableListOf<Particle2D>()
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            if (p.type == 0 && p.y >= height - 1.5f) { // Drop hit the ground
                splashes.add(p)
                iterator.remove()
            } else if (!p.active) {
                iterator.remove()
            }
        }
        
        // Create splash particles
        for (drop in splashes) {
            createSplash(drop)
        }
        
        // Render
        fadeToBlackBy(30)
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
    
    private fun drip() {
        val x = Random.nextFloat() * width
        val y = 0f
        val vx = 0f
        val vy = Random.nextFloat() * 5f
        
        val color = getColorFromPalette(Random.nextInt(255))
        
        val drop = Particle2D(x, y, vx, vy, color = color, lifetime = 2f, decayRate = 0.0f)
        drop.type = 0 // Main Drop
        drop.bounceX = false
        drop.bounceY = false
        drop.groundCollision = false
        particles.add(drop)
    }
    
    private fun createSplash(drop: Particle2D) {
        val numSplashes = 3 + Random.nextInt(4)
        val baseColor = drop.color
        
        for (i in 0 until numSplashes) {
            val angle = Math.PI + (Random.nextFloat() * Math.PI) // Upward hemisphere
            val speed = 8f + Random.nextFloat() * 10f
            val vx = (cos(angle) * speed).toFloat()
            val vy = (sin(angle) * speed).toFloat()
            
            val splash = Particle2D(drop.x, height - 1f, vx, vy, color = baseColor, lifetime = 1f, decayRate = 1.0f)
            splash.type = 1 // Splash droplet
            splash.bounceX = true
            splash.bounceY = false
            splash.groundCollision = true
            splash.size = 2 // Splash drops are smaller
            particles.add(splash)
        }
    }
}
