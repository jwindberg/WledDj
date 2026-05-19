package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.random.Random

class PsChaseAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // Zero gravity, particles are entirely kinematic and driven by chasing logic
        physics = PhysicsEnvironment(width, height, gravityX = 0f, gravityY = 0f, restitution = 1f, friction = 0.90f)
        lastUpdate = System.currentTimeMillis()
        
        // Create 20 particles
        for (i in 0 until 20) {
            val color = getColorFromPalette((i * 12) % 255)
            val p = Particle2D(width / 2f, height / 2f, 0f, 0f, color = color, lifetime = 1f, decayRate = 0f)
            p.bounceX = true
            p.bounceY = true
            p.size = 3
            particles.add(p)
        }
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // The leader (particle 0) moves randomly
        val leader = particles[0]
        if (Random.nextFloat() < 0.1f) {
            leader.vx += (Random.nextFloat() - 0.5f) * 100f
            leader.vy += (Random.nextFloat() - 0.5f) * 100f
        }
        
        // Ensure leader doesn't stop
        if (Math.abs(leader.vx) < 10f) leader.vx = (Random.nextFloat() - 0.5f) * 50f
        if (Math.abs(leader.vy) < 10f) leader.vy = (Random.nextFloat() - 0.5f) * 50f
        
        // Followers chase the particle directly in front of them in the array
        val chaseSpeed = 150f
        for (i in 1 until particles.size) {
            val follower = particles[i]
            val target = particles[i - 1]
            
            val dx = target.x - follower.x
            val dy = target.y - follower.y
            val distSq = dx * dx + dy * dy
            
            // Only accelerate if they are separated to avoid bunching up completely
            if (distSq > 4f) {
                val dist = kotlin.math.sqrt(distSq)
                follower.vx += (dx / dist) * chaseSpeed * dt
                follower.vy += (dy / dist) * chaseSpeed * dt
            }
        }
        
        physics.updateParticles(particles, dt)
        
        fadeToBlackBy(60)
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
}
