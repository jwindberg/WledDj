package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.min

/**
 * FireNoise2D - 2D fire effect with Perlin noise
 * Migrated to WledDj.
 */
class FireNoiseAnimation : BasePixelAnimation() {

    private var startTimeNs: Long = 0L
    private var accumulatedTime: Double = 0.0
    private var lastTimeMs: Long = 0
    
    // Fire palette colors
    private val firePalette = intArrayOf(
        Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
        Color.rgb(255, 0, 0), Color.rgb(255, 0, 0), Color.rgb(255, 0, 0), Color.rgb(255, 69, 0),
        Color.rgb(255, 69, 0), Color.rgb(255, 69, 0), Color.rgb(255, 165, 0), Color.rgb(255, 165, 0),
        Color.rgb(255, 255, 0), Color.rgb(255, 165, 0), Color.rgb(255, 255, 0), Color.rgb(255, 255, 0)
    )

    override fun onInit() {
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = System.nanoTime()
        
        if (lastTimeMs == 0L) lastTimeMs = now
        val dt = (now - lastTimeMs) / 1000.0
        lastTimeMs = now

        // Speed Control: Slider now controls Upward Motion Speed
        val speedMult = 0.5 + (paramSpeed / 255.0) * 2.5 // 0.5x to 3.0x
        accumulatedTime += dt * speedMult

        val timeVal = accumulatedTime

        // Auto-varying Flame Shape (yscale)
        // Morphs slowly over time (every ~10 seconds)
        // 5..30 range
        val autoScale = 15.0 + Math.sin(timeVal * 0.5) * 10.0 
        val yscale = autoScale.toInt().coerceIn(5, 30)
        
        // Intensity controls feature size (xscale) - keep this manual? 
        // Or auto-vary this too? User said "flame shapes", implying yscale.
        val xscale = 10 + (paramIntensity * 50 / 255)

        for (j in 0 until width) { // j is X
            for (i in 0 until height) { // i is Y
                // Use accumulatedTime for smooth scrolling
                // Factor 20.0 roughly matches previous /80 scale if dt is seconds
                val indexx = MathUtils.inoise8(j * yscale * height / 255, (i * xscale + timeVal * 200).toInt())
                
                // Fix: Divisor was width, should be height (since i is Y).
                // Also, saturation was too high.
                // paletteIndex logic:
                // i (0..H) -> 0..255.
                // noise (0..255).
                // We want: Bottom = Hot, Top = Cool.
                // Subtracting noise to create "gaps" in the fire.
                val rowPos = i * 255 / height
                val noiseVal = indexx // 0..255
                // Formula: Position - (Random Noise). 
                // At bottom (255), we subtract up to 100 to modulate.
                val paletteIndex = (rowPos - (255 - noiseVal)).coerceIn(0, 255)
                
                // Brightness also ramps with height
                val brightness = rowPos.coerceIn(0, 255)
                
                // Colors
                val pIdx = ((paletteIndex % 256) * firePalette.size / 256).coerceIn(0, firePalette.size - 1)
                val baseColor = firePalette[pIdx]
                val scaledColor = fadeColor2(baseColor, 255 - brightness) // fade amount is inverted brightness
                setPixelColor(j, i, scaledColor)
            }
        }
        // Render Sparks
        updateAndDrawSparks()
        return true
    }

    private data class Spark(var x: Float, var y: Float, var speed: Float, var life: Float)
    private val sparks = java.util.ArrayList<Spark>()

    private fun updateAndDrawSparks() {
        // Spawn
        // Chance linked to Intensity
        if (Math.random() * 255 < (paramIntensity / 10.0)) {
            sparks.add(Spark(
                x = (Math.random() * width).toFloat(),
                y = (height - 1 - (Math.random() * height * 0.5)).toFloat(), // Bottom half
                speed = (0.2 + Math.random() * 0.5).toFloat(),
                life = 1.0f
            ))
        }

        val iter = sparks.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            s.y -= s.speed
            s.life -= 0.02f
            
            // Wiggle
            s.x += (Math.random() - 0.5).toFloat() * 0.5f

            if (s.y < 0 || s.life <= 0) {
                iter.remove()
            } else {
                val ix = s.x.toInt()
                val iy = s.y.toInt()
                if (ix in 0 until width && iy in 0 until height) {
                    // Draw spark (Additive)
                    val color = if (s.life > 0.5) Color.YELLOW else Color.RED
                    // Simple blend
                    val current = getPixelColor(ix, iy)
                    // If current is black/dark, draw spark
                    if (Color.red(current) < 100) {
                         setPixelColor(ix, iy, color)
                    }
                }
            }
        }
    }

    // Helper to dim color (similar to scaleBrightness)
    private fun fadeColor2(color: Int, amount: Int): Int {
        val scale = (255 - amount).coerceAtLeast(0) / 255.0
        return Color.rgb(
           (Color.red(color) * scale).toInt(),
           (Color.green(color) * scale).toInt(),
           (Color.blue(color) * scale).toInt()
        )
    }
}
