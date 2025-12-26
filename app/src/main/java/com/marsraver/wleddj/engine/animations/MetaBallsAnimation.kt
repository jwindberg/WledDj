package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * MetaBalls animation - Blob-like effect with 3 moving points
 * Migrated to WledDj.
 */
class MetaBallsAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var startTimeNs: Long = 0L

    override fun onInit() {
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = System.nanoTime()
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000

        // Speed: 0.25f * (1+(speed>>6))
        val speedFactor = 0.25f * (1 + (paramSpeed shr 6))
        val timeValue = (timeMs * speedFactor).toInt()

        // Get 2 random moving points using perlin8
        // We simulate perlin8(t, c, c) using MathUtils.inoise8(t, c)
        // Adjust inputs to map to inoise8 expectations
        val x2 = MathUtils.map(MathUtils.inoise8(timeValue, 25355 + 685), 0, 255, 0, width - 1)
        val y2 = MathUtils.map(MathUtils.inoise8(timeValue, 355 + 11685), 0, 255, 0, height - 1)

        val x3 = MathUtils.map(MathUtils.inoise8(timeValue, 55355 + 6685), 0, 255, 0, width - 1)
        val y3 = MathUtils.map(MathUtils.inoise8(timeValue, 25355 + 22685), 0, 255, 0, height - 1)

        // One Lissajou function
        val x1 = MathUtils.beatsin8((23 * speedFactor).toInt(), 0, width - 1, timeMs)
        val y1 = MathUtils.beatsin8((28 * speedFactor).toInt(), 0, height - 1, timeMs)

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Calculate distances of the 3 points from actual pixel
                // Point 1 (Lissajou) - 2x weight
                var dx = abs(x - x1)
                var dy = abs(y - y1)
                var dist = 2 * sqrt(((dx * dx) + (dy * dy)).toDouble()).toInt()

                // Point 2 (Perlin)
                dx = abs(x - x2)
                dy = abs(y - y2)
                dist += sqrt(((dx * dx) + (dy * dy)).toDouble()).toInt()

                // Point 3 (Perlin)
                dx = abs(x - x3)
                dy = abs(y - y3)
                dist += sqrt(((dx * dx) + (dy * dy)).toDouble()).toInt()

                // Inverse result
                val colorVal = if (dist > 0) 1000 / dist else 255

                // Map color between thresholds
                if (colorVal > 0 && colorVal < 60) {
                    val paletteIndex = MathUtils.map(colorVal * 9, 9, 531, 0, 255)
                    // Use palette or fallback
                    // Use palette
                    val pixelColor = getColorFromPalette(paletteIndex)
                    setPixelColor(x, y, pixelColor)
                } else {
                    setPixelColor(x, y, Color.BLACK)
                }
            }
        }

        // Show the 3 points in white (optional, as per source)
        setPixelColor(x1, y1, Color.WHITE)
        setPixelColor(x2, y2, Color.WHITE)
        setPixelColor(x3, y3, Color.WHITE)

        return true
    }
}
