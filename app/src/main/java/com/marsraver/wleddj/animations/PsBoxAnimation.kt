package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import kotlin.random.Random

class PsBoxAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private var resetTimer: Long = 0

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // High elasticity so the box bounces around for a while
        physics = PhysicsEnvironment(width, height, gravityX = 0f, gravityY = 15f, restitution = 0.85f, friction = 0.99f)
        lastUpdate = System.currentTimeMillis()
        
        spawnBox()
    }
    
    private fun spawnBox() {
        particles.clear()
        
        val boxWidth = 10
        val boxHeight = 10
        val startX = width / 2f - boxWidth / 2f
        val startY = height / 4f
        
        val initialVx = (Random.nextFloat() - 0.5f) * 40f
        val initialVy = -Random.nextFloat() * 20f // slight jump
        
        for (y in 0 until boxHeight) {
            for (x in 0 until boxWidth) {
                // Only spawn the outline of the box
                if (x == 0 || x == boxWidth - 1 || y == 0 || y == boxHeight - 1) {
                    val color = getColorFromPalette(((x + y) * 10) % 255)
                    val p = Particle2D(startX + x * 2f, startY + y * 2f, initialVx, initialVy, color = color, lifetime = 1f, decayRate = 0f)
                    p.bounceX = true
                    p.groundCollision = true
                    p.size = 2
                    particles.add(p)
                }
            }
        }
        
        resetTimer = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        // The box will eventually collapse into a pile of bouncing dots. Reset it every 8 seconds.
        if (now - resetTimer > 8000) {
            spawnBox()
        }
        
        physics.updateParticles(particles, dt)
        
        fadeToBlackBy(80)
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        
        return true
    }
}
