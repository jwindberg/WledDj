package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.random.Random

class PsHourglassAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        physics = PhysicsEnvironment(width, height, gravityY = 20f, restitution = 0.2f, friction = 0.8f)
        lastUpdate = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // Hourglass logic: spawn slowly, they fall and pile up (they don't decay)
        if (particles.size < 120 && Random.nextFloat() < 0.2f) {
            val color = getColorFromPalette(Random.nextInt(255))
            val startY = if (physics.gravityY > 0) 0f else height - 1f
            val vy = if (physics.gravityY > 0) Random.nextFloat() * 5f else -Random.nextFloat() * 5f
            
            val p = Particle2D(width / 2f + (Random.nextFloat() - 0.5f) * 4f, startY, 0f, vy, color = color, lifetime = 1f, decayRate = 0f)
            p.bounceX = true
            p.groundCollision = true
            p.size = 2
            particles.add(p)
        }
        
        // If full, flip gravity
        if (particles.size >= 120) {
            physics.gravityY = -physics.gravityY
            for (p in particles) {
                p.color = getColorFromPalette(Random.nextInt(255)) // Change colors on flip
            }
            // Thin out slightly to restart flow
            particles.removeAll { Random.nextFloat() < 0.1f } 
        }
        
        physics.updateParticles(particles, dt)
        
        // Fake pile-up by stopping particles that hit the "bottom"
        for (p in particles) {
            if (physics.gravityY > 0 && p.y >= height - 2f) {
                p.vy *= 0.5f
                p.y = height - 1.5f + (Random.nextFloat() - 0.5f) * 2f
            } else if (physics.gravityY < 0 && p.y <= 2f) {
                p.vy *= 0.5f
                p.y = 1.5f + (Random.nextFloat() - 0.5f) * 2f
            }
        }
        
        fadeToBlackBy(60)
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
}
