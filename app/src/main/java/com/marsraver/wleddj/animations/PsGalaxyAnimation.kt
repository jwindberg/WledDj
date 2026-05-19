package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PsGalaxyAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private var angleOffset = 0f

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        physics = PhysicsEnvironment(width, height, gravityX = 0f, gravityY = 0f, restitution = 0f, friction = 0.99f)
        physics.pointGravityEnabled = true
        physics.pointGravityX = width / 2f
        physics.pointGravityY = height / 2f
        physics.pointGravityMass = 250f // Inward pull
        
        lastUpdate = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        angleOffset += dt * 4f
        
        // Spawn particles in center to create a spiral galaxy
        if (particles.size < 250 && Random.nextFloat() < 0.6f) {
            val spawnAngle = angleOffset + (Random.nextFloat() - 0.5f) * 0.5f
            
            val radiusAngle = spawnAngle
            val tangentialAngle = radiusAngle + Math.PI.toFloat() / 2f
            
            val outwardSpeed = 8f
            val tangentialSpeed = 22f
            
            val vx = (cos(radiusAngle) * outwardSpeed + cos(tangentialAngle) * tangentialSpeed).toFloat()
            val vy = (sin(radiusAngle) * outwardSpeed + sin(tangentialAngle) * tangentialSpeed).toFloat()
            
            val color = getColorFromPalette((System.currentTimeMillis() / 40 % 255).toInt())
            
            val p = Particle2D(width / 2f, height / 2f, vx, vy, color = color, lifetime = 1f, decayRate = 0.05f)
            p.bounceX = false
            p.bounceY = false
            p.size = if (Random.nextFloat() > 0.85f) 3 else 2 // Chunkier stars
            particles.add(p)
        }
        
        physics.updateParticles(particles, dt)
        
        // Remove particles that fly out of bounds
        particles.removeAll { 
            !it.active || 
            it.x < -20f || it.x > width + 20f || 
            it.y < -20f || it.y > height + 20f 
        }
        
        fadeToBlackBy(40)
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
}
