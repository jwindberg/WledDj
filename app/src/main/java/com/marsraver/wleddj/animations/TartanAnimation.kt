package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

/**
 * Tartan animation - animated plaid pattern
 * Migrated to WledDj.
 */
class TartanAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var horizontalScale: Double = 1.0
    private var hueBase: Int = 0
    private var hueAccumulatorMs: Double = 0.0
    private var lastUpdateNanos: Long = 0L

    override fun onInit() {
        offsetX = 0.0
        offsetY = 0.0
        horizontalScale = 1.0
        hueBase = 0
        hueAccumulatorMs = 0.0
        lastUpdateNanos = 0L
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now
        }
        val deltaMs = (now - lastUpdateNanos) / 1_000_000.0
        lastUpdateNanos = now
        
        val speedFactor = paramSpeed / 128.0
        val incrementThreshold = 8.0 / speedFactor.coerceAtLeast(0.1)
        
        hueAccumulatorMs += deltaMs
        while (hueAccumulatorMs >= incrementThreshold) {
            hueBase = (hueBase + 1) and 0xFF
            hueAccumulatorMs -= incrementThreshold
        }

        // Use now for absolute time driving beatsins
        val timeSeconds = now / 1_000_000_000.0
        val freqScale = paramSpeed / 128.0
        
        // Boosted frequencies to make movement visible
        offsetX = beatsin(timeSeconds, 1.0 * freqScale, -180.0, 180.0)
        offsetY = beatsin(timeSeconds, 0.75 * freqScale, -180.0, 180.0, phaseOffsetSeconds = 6.0)
        horizontalScale = beatsin(timeSeconds, 1.4 * freqScale, 0.5, 4.0)
        
        for (x in 0 until width) {
            val hueX = (x * horizontalScale + offsetY + hueBase).toFloat()
            val brightnessX = sin8(x * 18.0 + offsetX)
            val colorX = getColorFromHue(hueX, brightnessX)
            
            for (y in 0 until height) {
                val hueY = (y * 2.0 + offsetX + hueBase).toFloat()
                val brightnessY = sin8(y * 18.0 + offsetY)
                val colorY = getColorFromHue(hueY, brightnessY)

                val r = (Color.red(colorX) + Color.red(colorY)).coerceAtMost(255)
                val g = (Color.green(colorX) + Color.green(colorY)).coerceAtMost(255)
                val b = (Color.blue(colorX) + Color.blue(colorY)).coerceAtMost(255)
                
                setPixelColor(x, y, Color.rgb(r, g, b))
            }
        }
        return true
    }

    private fun beatsin(
        timeSeconds: Double,
        frequencyHz: Double,
        low: Double,
        high: Double,
        phaseOffsetSeconds: Double = 0.0,
    ): Double {
        val angle = 2.0 * PI * (frequencyHz * (timeSeconds + phaseOffsetSeconds))
        val sine = sin(angle)
        return low + (high - low) * (sine + 1.0) / 2.0
    }

    private fun sin8(input: Double): Int {
        var angle = input % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * PI
        val sine = sin(radians)
        return ((sine + 1.0) * 127.5).roundToInt().coerceIn(0, 255)
    }

    private fun getColorFromHue(hue: Float, brightness: Int): Int {
        val h = (hue.toInt() % 256 + 256) % 256
        val color = getColorFromPalette(h)
        // fadeColor expects 'amount to subtract/fade', so 255-brightness
        // If brightness is 255, fade is 0 (full color).
        // If brightness is 0, fade is 255 (black).
        return fadeColor(color, 255 - brightness)
    }
}
