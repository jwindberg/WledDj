package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.math.cos
import kotlin.math.sin

class PsGhostRiderAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private var timeTracker: Float = 0f

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        physics = PhysicsEnvironment(width, height, gravityY = 0f, restitution = 1.0f, friction = 1.0f)
        lastUpdate = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        timeTracker += dt * 8f
        
        // Spawn particles in a Lissajous figure to simulate the swinging chain
        val x = width / 2f + (cos(timeTracker * 0.8f) * (width / 2.5f)).toFloat()
        val y = height / 2f + (sin(timeTracker * 1.1f) * (height / 2.5f)).toFloat()
        
        // Color cycles quickly
        val color = getColorFromPalette((timeTracker * 20).toInt() % 255)
        
        val spark = Particle2D(x, y, 0f, 0f, color = color, lifetime = 1f, decayRate = 0.04f)
        spark.size = 3
        particles.add(spark)
        
        physics.updateParticles(particles, dt)
        particles.removeAll { !it.active }
        
        fadeToBlackBy(15) // Long trail
        
        for (p in particles) {
            val dimColor = fadeColor(p.color, ((1f - p.lifetime) * 200).toInt().coerceIn(0, 255))
            drawBlob(p.x, p.y, dimColor, p.size)
        }
        
        return true
    }
}
