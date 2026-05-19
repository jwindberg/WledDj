package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.math.sqrt
import kotlin.random.Random

class PsSpringyAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        physics = PhysicsEnvironment(width, height, gravityX = 0f, gravityY = 20f, restitution = 0.8f, friction = 0.95f)
        lastUpdate = System.currentTimeMillis()
        
        // Create a chain of particles
        for (i in 0 until 5) {
            val p = Particle2D(width / 2f + i * 2f, height / 2f, 0f, 0f, color = 0, lifetime = 1f, decayRate = 0f)
            p.bounceX = true
            p.groundCollision = true
            p.size = 4
            particles.add(p)
        }
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // Anchor the first particle to top center
        particles[0].x = width / 2f
        particles[0].y = 0f
        particles[0].vx = 0f
        particles[0].vy = 0f
        
        val springK = 30f // Softer spring constant
        val restLength = 2f
        val dampening = 0.5f // Much less internal friction
        
        // Randomly kick the bottom particle to keep the spring system lively
        if (Random.nextFloat() < 0.05f) {
            particles.last().vx += (Random.nextFloat() - 0.5f) * 80f
            particles.last().vy -= Random.nextFloat() * 40f
        }
        
        // Apply spring forces between adjacent particles
        for (i in 0 until particles.size - 1) {
            val p1 = particles[i]
            val p2 = particles[i + 1]
            
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val distSq = dx * dx + dy * dy
            val dist = sqrt(distSq).coerceAtLeast(0.1f)
            
            val force = -springK * (dist - restLength)
            
            // Normalize direction
            val nx = dx / dist
            val ny = dy / dist
            
            // Spring force vector
            val fx = nx * force
            val fy = ny * force
            
            // Dampening force (relative velocity)
            val dvx = p2.vx - p1.vx
            val dvy = p2.vy - p1.vy
            val dampX = -dampening * dvx
            val dampY = -dampening * dvy
            
            val totalFx = fx + dampX
            val totalFy = fy + dampY
            
            // Apply equal and opposite forces
            if (i > 0) { // Don't move anchor
                p1.vx -= totalFx * dt
                p1.vy -= totalFy * dt
            }
            p2.vx += totalFx * dt
            p2.vy += totalFy * dt
        }
        
        // Update physics
        physics.updateParticles(particles, dt)
        
        fadeToBlackBy(80)
        
        val timeOffset = (System.currentTimeMillis() / 20) % 255
        
        for (i in 0 until particles.size) {
            val p = particles[i]
            val colorIndex = (timeOffset + i * 40).toInt() % 255
            p.color = getColorFromPalette(colorIndex)
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
}
