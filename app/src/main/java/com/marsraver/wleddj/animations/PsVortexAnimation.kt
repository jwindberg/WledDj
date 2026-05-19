package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PsVortexAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        physics = PhysicsEnvironment(width, height, gravityX = 0f, gravityY = 0f, restitution = 0f, friction = 0.98f)
        physics.pointGravityEnabled = true
        physics.pointGravityX = width / 2f
        physics.pointGravityY = height / 2f
        physics.pointGravityMass = 600f // Strong inward pull
        
        lastUpdate = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // Spawn particles at random edge locations
        if (particles.size < 180) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val radius = width.coerceAtLeast(height).toFloat() + 5f // start just outside the screen
            val spawnX = width / 2f + cos(angle) * radius
            val spawnY = height / 2f + sin(angle) * radius
            
            // Tangential velocity to make it spiral instead of falling straight in
            val tangentialAngle = angle + Math.PI.toFloat() / 2f
            val tangentialSpeed = 25f + Random.nextFloat() * 15f
            val vx = (cos(tangentialAngle) * tangentialSpeed).toFloat()
            val vy = (sin(tangentialAngle) * tangentialSpeed).toFloat()
            
            val color = getColorFromPalette(Random.nextInt(255))
            
            val p = Particle2D(spawnX, spawnY, vx, vy, color = color, lifetime = 1f, decayRate = 0.0f) // No decay
            p.bounceX = false
            p.bounceY = false
            p.size = 3
            particles.add(p)
        }
        
        physics.updateParticles(particles, dt)
        
        // Remove particles that got sucked into the exact center
        val cx = width / 2f
        val cy = height / 2f
        particles.removeAll { 
            val dx = it.x - cx
            val dy = it.y - cy
            (dx * dx + dy * dy) < 4f 
        }
        
        fadeToBlackBy(60)
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
}
