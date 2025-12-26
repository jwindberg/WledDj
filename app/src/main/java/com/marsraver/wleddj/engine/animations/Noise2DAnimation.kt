package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils

/**
 * Noise2D - Basic 2D Perlin noise pattern
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palettes

class Noise2DAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var startTimeNs: Long = 0L

    override fun onInit() {
        startTimeNs = System.nanoTime()
        currentPalette = Palettes.get("Rainbow")
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = System.nanoTime()
        
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000
        val scale = paramIntensity + 2
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Use MathUtils.inoise8 3D
                val denominator = 16 - paramSpeed / 16
                val zTime = if(denominator != 0) (timeMs / denominator).toInt() else timeMs.toInt()
                
                val pixelHue8 = MathUtils.inoise8(x * scale, y * scale, zTime)
                
                // Color map: Rainbow or Palette
                val color = getColorFromPalette(pixelHue8)
                setPixelColor(x, y, color)
            }
        }
        return true
    }
}
