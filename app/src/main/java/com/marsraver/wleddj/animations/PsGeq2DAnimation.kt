package com.marsraver.wleddj.animations

import com.marsraver.wleddj.animations.physics.Particle2D
import com.marsraver.wleddj.animations.physics.PhysicsEnvironment
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.random.Random

class PsGeq2DAnimation : BasePixelAnimation() {

    private lateinit var physics: PhysicsEnvironment
    private val particles = mutableListOf<Particle2D>()
    private var lastUpdate: Long = 0
    private var fftMeter: FftMeter? = null
    private val noiseFloor = 20

    override fun supportsSpeed(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        physics = PhysicsEnvironment(width, height, gravityY = 25f, restitution = 0.4f, friction = 0.9f)
        lastUpdate = System.currentTimeMillis()
        fftMeter = FftMeter(bands = 256)
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        
        val spectrum = fftMeter?.getNormalizedBands() ?: IntArray(width)
        
        // Spawn particles based on EQ
        val spawnRateMod = paramSpeed / 128f
        for (x in 0 until width step 2) { // Step 2 to avoid overcrowding
            val bandIndex = mapValue(x, 0, width, 0, 80)
            val amplitude = spectrum.getOrElse(bandIndex) { 0 }
            
            // normalized amplitude > 120 means strong frequency hit
            if (amplitude > 120 && Random.nextFloat() < 0.2f * spawnRateMod) {
                // High amplitude triggers a particle fountain from the bottom
                val vy = -25f - (amplitude / 255f) * 35f // Upward velocity based on volume
                
                // Color based on x position and time
                val timeOffset = (System.currentTimeMillis() / 20) % 255
                val colorIndex = (x * 8 + timeOffset).toInt() % 255
                val color = getColorFromPalette(colorIndex)
                
                val p = Particle2D(x.toFloat(), height - 1f, (Random.nextFloat() - 0.5f) * 4f, vy, color = color, lifetime = 1f, decayRate = 0.05f)
                p.bounceX = true
                p.bounceY = false
                p.groundCollision = true
                particles.add(p)
            }
        }
        
        // Update physics
        physics.updateParticles(particles, dt)
        particles.removeAll { !it.active }
        
        // Render
        fadeToBlackBy(50)
        
        for (p in particles) {
            drawBlob(p.x, p.y, p.color, p.size)
        }
        return true
    }
    
    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
    }
    
    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
