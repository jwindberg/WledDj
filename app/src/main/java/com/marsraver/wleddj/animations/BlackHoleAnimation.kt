package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.max
import kotlin.math.min

/**
 * Black Hole animation - Orbiting stars around a central point.
 * Migrated to WledDj.
 */
class BlackHoleAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true
    override fun getDefaultPaletteName(): String = "Rainbow"

    private var solid: Boolean = false
    private var blur: Boolean = true // Enabled by default in source logic (though var said false, check logic used custom1)
    private var startTime: Long = 0
    private var startTimeNs: Long = 0L

    override fun onInit() {
        startTimeNs = System.nanoTime()
        startTime = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000
        
        val fadeAmount = max(2, (16 + (paramSpeed shr 3)) / 4)
        fadeToBlackBy(fadeAmount)

        val t = timeMs / 128
        val custom1 = 128
        val custom2 = 128

        for (i in 0 until 8) {
            val phaseOffsetX = if (i % 2 == 1) 128 else 0
            val phaseOffsetY = if (i % 2 == 1) 192 else 64
            val x = MathUtils.beatsin8(custom1 shr 3, 0, width - 1, t, phaseOffsetX + (t * i).toInt())
            val y = MathUtils.beatsin8(paramIntensity shr 3, 0, height - 1, t, phaseOffsetY + (t * i).toInt())

            val paletteIndex = i * 32
            val brightness = if (solid) 0 else 255
            val color = if (brightness > 0) getColorFromPalette(paletteIndex) else Color.BLACK
            
            addPixelColor(x, y, color)
        }

        val innerFreq = custom2 shr 3
        for (i in 0 until 4) {
            val phaseOffsetX = if (i % 2 == 1) 128 else 0
            val phaseOffsetY = if (i % 2 == 1) 192 else 64
            val minX = width / 4
            val maxX = width - 1 - width / 4
            val minY = height / 4
            val maxY = height - 1 - height / 4

            val x = MathUtils.beatsin8(innerFreq, minX, maxX, t, phaseOffsetX + (t * i).toInt())
            val y = MathUtils.beatsin8(innerFreq, minY, maxY, t, phaseOffsetY + (t * i).toInt())

            val paletteIndex = 255 - i * 64
            val brightness = if (solid) 0 else 255
            val color = if (brightness > 0) getColorFromPalette(paletteIndex) else Color.BLACK
            
            addPixelColor(x, y, color)
        }

        val centerX = width / 2
        val centerY = height / 2
        setPixelColor(centerX, centerY, Color.WHITE)

        if (blur) {
            val blurAmount = 16
            // useSmear = true if small grid?
            // implemented in blur2d? No. BasePixelAnimation blur2d is simple box blur.
            blur2d(blurAmount)
        }
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
