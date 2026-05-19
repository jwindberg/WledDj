package com.marsraver.wleddj.animations.physics

import kotlin.math.pow

class PhysicsEnvironment(
    var width: Int,
    var height: Int,
    var gravityX: Float = 0f,
    var gravityY: Float = 9.8f, // Positive Y is down
    var friction: Float = 0.99f,
    var restitution: Float = 0.8f, // Bounciness
    var pointGravityEnabled: Boolean = false,
    var pointGravityX: Float = 0f,
    var pointGravityY: Float = 0f,
    var pointGravityMass: Float = 1000f
) {
    fun updateParticle(p: Particle2D, dt: Float) {
        if (!p.active) return
        
        // Apply forces
        p.vx += gravityX * dt
        p.vy += gravityY * dt
        
        if (pointGravityEnabled) {
            val dx = pointGravityX - p.x
            val dy = pointGravityY - p.y
            val distSq = (dx * dx + dy * dy).coerceAtLeast(1f) // Prevent div by zero
            val dist = kotlin.math.sqrt(distSq)
            val force = pointGravityMass / distSq
            p.vx += (dx / dist) * force * dt
            p.vy += (dy / dist) * force * dt
        }
        
        // Apply friction (retained velocity per second)
        val f = friction.pow(dt)
        p.vx *= f
        p.vy *= f
        
        // Update position
        p.x += p.vx * dt
        p.y += p.vy * dt
        
        // Decay
        p.lifetime -= p.decayRate * dt
        if (p.lifetime <= 0f) {
            p.active = false
            return
        }
        
        // Collisions (Bounding Box)
        if (p.bounceX) {
            if (p.x < 0) {
                p.x = 0f
                p.vx = -p.vx * restitution
            } else if (p.x >= width) {
                p.x = width - 1f
                p.vx = -p.vx * restitution
            }
        }
        
        if (p.bounceY || p.groundCollision) {
            if (p.bounceY && p.y < 0) {
                p.y = 0f
                p.vy = -p.vy * restitution
            }
            if (p.groundCollision && p.y >= height) {
                p.y = height - 1f
                p.vy = -p.vy * restitution
            }
        }
    }
    
    fun updateParticles(particles: List<Particle2D>, dt: Float) {
        for (p in particles) {
            updateParticle(p, dt)
        }
    }
}
