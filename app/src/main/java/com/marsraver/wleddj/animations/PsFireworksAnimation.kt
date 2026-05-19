package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PsFireworksAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // Lower gravity and higher friction for floaty sparks
        physics = PhysicsEnvironment(width, height, gravityY = 15f, restitution = 0.5f, friction = 0.6f)
        lastUpdate = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // Spawn shells (Type 0 = Shell, Type 1 = Spark)
        val chanceToLaunch = (paramIntensity / 255f) * 0.05f
        if (Random.nextFloat() < chanceToLaunch && particles.count { it.type == 0 } < 5) {
            launchShell()
        }
        
        // Update physics
        physics.updateParticles(particles, dt)
        
        // Process explosions and dead particles
        val explosions = mutableListOf<Particle2D>()
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            if (p.type == 0 && p.vy >= -2f) { // Explode near apex
                explosions.add(p)
                iterator.remove()
            } else if (!p.active) {
                iterator.remove()
            }
        }
        
        // Create sparks
        for (shell in explosions) {
            explode(shell)
        }
        
        // Render
        fadeToBlackBy(45)
        
        for (p in particles) {
            // Dim color slightly based on lifetime for sparks
            val color = if (p.type == 1) fadeColor(p.color, ((1f - p.lifetime) * 100).toInt()) else p.color
            drawBlob(p.x, p.y, color, p.size)
        }
        
        return true
    }
    
    private fun launchShell() {
        val x = 2f + Random.nextFloat() * (width - 4f)
        val y = height - 1f
        val vx = (Random.nextFloat() - 0.5f) * 6f
        val vy = -30f - Random.nextFloat() * 15f // Shoot up fast
        
        val color = getColorFromPalette(Random.nextInt(255))
        
        val shell = Particle2D(x, y, vx, vy, color = color, lifetime = 2f, decayRate = 0.0f)
        shell.type = 0 // Shell
        shell.bounceY = false
        shell.groundCollision = false
        particles.add(shell)
    }
    
    private fun explode(shell: Particle2D) {
        val numSparks = 20 + Random.nextInt(30)
        val baseColor = shell.color
        
        for (i in 0 until numSparks) {
            val angle = Random.nextFloat() * Math.PI * 2
            val speed = 8f + Random.nextFloat() * 20f
            val vx = (cos(angle) * speed).toFloat()
            val vy = (sin(angle) * speed).toFloat()
            
            val spark = Particle2D(shell.x, shell.y, vx, vy, color = baseColor, lifetime = 1f, decayRate = 0.4f + Random.nextFloat() * 0.6f)
            spark.type = 1 // Spark
            spark.bounceX = false
            spark.bounceY = false
            spark.groundCollision = false
            spark.size = 2
            particles.add(spark)
        }
    }
}
