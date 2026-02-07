package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.min

/**
 * Frizzles animation - Bouncing pixels with trails.
 * Migrated to WledDj.
 */
class FrizzlesAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // No special init needed
    }

    override fun update(now: Long): Boolean {
        // fade
        fadeToBlackBy(16)

        // Use millis directly as 'timeMs' for beatsin8
        val timeMs = now
        val loops = 8
        
        // Map paramSpeed (0-255) to Frequency (4-42)
        val speedVal = if (paramSpeed == 0) 1 else paramSpeed
        val baseFreq = 4 + (speedVal * 38) / 255
        
        for (i in loops downTo 1) {
            val freqBase = baseFreq
            val x = MathUtils.beatsin8(freqBase + i, 0, width - 1, timeMs)
            val y = MathUtils.beatsin8(15 - i, 0, height - 1, timeMs)
            val hue = MathUtils.beatsin8(freqBase, 0, 255, timeMs)
            val color = getColorFromPalette(hue)
            
            addPixelColor(x, y, color)

            if (width > 24 || height > 24) {
                addPixelColor(x + 1, y, color)
                addPixelColor(x - 1, y, color)
                addPixelColor(x, y + 1, color)
                addPixelColor(x, y - 1, color)
            }
        }
        
        blur2d(16)
        return true
    }
    
    private fun addPixelColor(x: Int, y: Int, color: Int) {
        if (x in 0 until width && y in 0 until height) {
            val current = getPixelColor(x, y)
             val r = min(Color.red(current) + Color.red(color), 255)
             val g = min(Color.green(current) + Color.green(color), 255)
             val b = min(Color.blue(current) + Color.blue(color), 255)
             setPixelColor(x, y, Color.rgb(r, g, b))
        }
    }
}
