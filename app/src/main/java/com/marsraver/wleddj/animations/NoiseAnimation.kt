package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.math.NoiseUtils
import com.marsraver.wleddj.engine.color.Palette

class NoiseAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var startTimeNs: Long = 0L

    override fun getDefaultPalette(): Palette = Palette.RAINBOW

    override fun onInit() {
        // Init noise array
    }

    // Params
    // paramSpeed is inherited from BasePixelAnimation (default 128)
    // paramIntensity is inherited from BasePixelAnimation (default 128)
    
    // Speed Support
    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        paramSpeed = (speed * 255f).toInt().coerceIn(0, 255)
    }
    override fun getSpeed(): Float = paramSpeed / 255f

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = System.nanoTime()
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000
        
        // Scale: Determines "Zoom Level" (Cloud Size)
        // NoiseUtils expects 256 units = 1 lattice cell.
        // We want ~2-5 cells across the width for nice clouds.
        // If width=80, we need range 0..~1000. So scale ~12.
        
        // Param Intensity: 0..255.
        // Map 0 -> Scale 2 (Huge clouds)
        // Map 255 -> Scale 50 (Dense clouds)
        val zoom = 2 + (paramIntensity / 5) 
        
        // Speed: Z-axis movement
        val speedFactor = if(paramSpeed < 10) 0 else paramSpeed
        
        // Z movement needs to be significant to show change, but smooth.
        // paramSpeed 255 -> Fast boil. 
        val zSpeed = speedFactor / 8.0f // 0..32 units per update?
        
        // We accumulate Z based on time to be frame-rate independent
        // Boosted speed: was 0.1, now 0.8 for faster boiling
        val zTime = (timeMs * (speedFactor / 255.0 * 0.8)).toInt()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Use scaled coordinates for smooth noise
                val nx = (x * zoom)
                val ny = (y * zoom)
                
                // NOISE UTILS (Real Perlin)
                val noiseVal = NoiseUtils.inoise8(nx, ny, zTime)
                
                // Map Noise (0-255) to Palette
                // Perlin output is often center-weighted (128).
                // To avoid "white lights" (desaturated look at ends of rainbow?),
                // we map directly.
                val color = getColorFromPalette(noiseVal)
                setPixelColor(x, y, color)
            }
        }
        return true
    }
}
