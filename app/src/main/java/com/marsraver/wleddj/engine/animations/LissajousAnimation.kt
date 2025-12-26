package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils

/**
 * Lissajous animation - Curved patterns
 * Migrated to WledDj.
 */
class LissajousAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var custom1: Int = 120   // Blur amount default
    private var custom3: Int = 50   // Rotation speed default
    private var check1: Boolean = false  // Smear mode

    private var startTimeNs: Long = 0L

    override fun onInit() {
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000

        // Fade to black by intensity
        fadeToBlackBy(paramIntensity)

        // Calculate phase
        val phase = ((timeMs * (1 + custom3)) / 32).toInt()

        for (i in 0 until 256) {
            val xlocn = MathUtils.sin8(phase / 2 + (i * paramSpeed) / 32)
            val ylocn = MathUtils.cos8(phase / 2 + i * 2)

            val x = if (width < 2) 1 else (MathUtils.map(2 * xlocn, 0, 511, 0, 2 * (width - 1)) + 1) / 2
            val y = if (height < 2) 1 else (MathUtils.map(2 * ylocn, 0, 511, 0, 2 * (height - 1)) + 1) / 2

            val colorIndex = ((timeMs / 100 + i) % 256).toInt()
            val color = getColorFromPalette(colorIndex)

            setPixelColor(x, y, color)
        }

        // Apply blur
        // WledFx custom1 shr (1 + check1*3). 
        // 120 -> 120/2 = 60.
        val blurAmount = custom1 shr (1 + if (check1) 3 else 0)
        if (blurAmount > 0) {
            blur2d(blurAmount)
        }

        return true
    }
}
