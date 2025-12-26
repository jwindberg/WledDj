package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Square Swirl animation - layered sine-driven points with dynamic blur.
 * Migrated to WledDj.
 */
class SquareSwirlAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        // BasePixelAnimation initializes pixels
    }

    override fun update(now: Long): Boolean {
        val timeMs = now / 1_000_000

        // Apply blurring
        // WledFx used beat8 for blur amount
        val blurWave = MathUtils.beatsin8(3, 64, 192, timeMs)
        val blurAmount = dim8Raw(blurWave)
        blur2d(blurAmount)

        val border = max(1, min(width, height) / 16)
        val maxX = (width - 1 - border).coerceAtLeast(border)
        val maxY = (height - 1 - border).coerceAtLeast(border)

        val maxCoord = min(maxX, maxY)
        val i = MathUtils.beatsin8(91, border, maxCoord, timeMs)
        val j = MathUtils.beatsin8(109, border, maxCoord, timeMs)
        val k = MathUtils.beatsin8(73, border, maxCoord, timeMs)

        val hue1 = ((timeMs / 29) % 256).toInt()
        val hue2 = ((timeMs / 41) % 256).toInt()
        val hue3 = ((timeMs / 73) % 256).toInt()

        addPixelColor(i.coerceIn(0, width - 1), j.coerceIn(0, height - 1), getColorFromPalette(hue1))
        addPixelColor(j.coerceIn(0, width - 1), k.coerceIn(0, height - 1), getColorFromPalette(hue2))
        addPixelColor(k.coerceIn(0, width - 1), i.coerceIn(0, height - 1), getColorFromPalette(hue3))

        return true
    }

    private fun dim8Raw(value: Int): Int {
        val v = value.coerceIn(0, 255)
        return ((v * v) / 255.0).roundToInt().coerceIn(0, 255)
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
