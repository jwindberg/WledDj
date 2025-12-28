package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.min

/**
 * FireNoise2D - 2D fire effect with Perlin noise
 * Migrated to WledDj.
 */
class FireNoise2DAnimation : BasePixelAnimation() {

    private var startTimeNs: Long = 0L
    
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
        
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000
        val xscale = paramIntensity * 4
        val yscale = paramSpeed * 8
        
        for (j in 0 until width) {
            for (i in 0 until height) {
                // Use MathUtils.inoise8
                val indexx = MathUtils.inoise8(j * yscale * height / 255, (i * xscale + timeMs / 4).toInt())
                val paletteIndex = min(i * (indexx shr 4), 255)
                val brightness = i * 255 / width
                
                // Colors
                val pIdx = ((paletteIndex % 256) * firePalette.size / 256).coerceIn(0, firePalette.size - 1)
                val baseColor = firePalette[pIdx]
                val scaledColor = fadeColor2(baseColor, 255 - brightness)
                setPixelColor(j, i, scaledColor)
            }
        }
        return true
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
