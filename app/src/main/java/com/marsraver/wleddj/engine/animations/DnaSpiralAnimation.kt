package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.*

/**
 * DNA Spiral animation
 * Migrated to WledDj.
 */
class DnaSpiralAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var lastUpdateNs: Long = 0L
    private var hueOffset: Int = 0
    
    private val FREQ = 6
    private val PERIOD_SECONDS = 5.5

    override fun onInit() {
        lastUpdateNs = 0L
        hueOffset = 0
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNs == 0L) lastUpdateNs = now
        fadeDown()

        val timeSeconds = now / 1_000_000_000.0
        val auxTime = timeSeconds * 0.85
        val rows = height
        val edgePadding = 1
        val center = (width - 1) / 2.0
        val amplitude = (width - 1 - edgePadding * 2) / 2.0

        val timeSegment = (timeSeconds / 0.008).toInt()
        for (row in 0 until rows) {
            val basePhase = row * FREQ
            val phase1 = basePhase
            val phase2 = basePhase + 128

            val p1 = center + sin(2 * PI * (timeSeconds / PERIOD_SECONDS + phase1 / 256.0)) * amplitude
            val p2 = center + sin(2 * PI * (auxTime / PERIOD_SECONDS + phase1 / 256.0 + 0.5)) * amplitude
            val q1 = center + sin(2 * PI * (timeSeconds / PERIOD_SECONDS + phase2 / 256.0)) * amplitude
            val q2 = center + sin(2 * PI * (auxTime / PERIOD_SECONDS + phase2 / 256.0 + 0.5)) * amplitude

            val x = ((p1 + p2) / 2.0).roundToInt().coerceIn(edgePadding, width - 1 - edgePadding)
            val x1 = ((q1 + q2) / 2.0).roundToInt().coerceIn(edgePadding, width - 1 - edgePadding)

            val hue = ((row * 128) / (rows.coerceAtLeast(2) - 1) + hueOffset) and 0xFF
            val color = getColorFromPalette(hue)

            if (((row + timeSegment) and 3) != 0) {
                drawLine(x, x1, row, color, addDot = true, gradient = true)
            }
        }
        hueOffset = (hueOffset + 3) and 0xFF
        return true
    }
    
    private fun drawLine(x0: Int, x1: Int, y: Int, rgb: Int, addDot: Boolean, gradient: Boolean) {
        val clampedY = y.coerceIn(0, height - 1)
        val start = x0.coerceIn(0, width - 1)
        val end = x1.coerceIn(0, width - 1)
        val steps = abs(end - start) + 1
        for (step in 0 until steps) {
            val t = step / (steps - 1.0).coerceAtLeast(1.0)
            val x = lerp(start, end, t).roundToInt().coerceIn(0, width - 1)
            val scale = if (gradient) (t * 255).roundToInt().coerceIn(0, 255) else 255
            val scaled = scaleColorBrightness(rgb, scale)
            addPixelColor(x, clampedY, scaled)
        }
        if (addDot) {
            val darkPurple = Color.rgb(72, 61, 139)
            addPixelColor(start.coerceIn(0, width - 1), clampedY, darkPurple)
            addPixelColor(end.coerceIn(0, width - 1), clampedY, Color.WHITE)
        }
    }

    private fun fadeDown() {
        // BasePixelAnimation pixels 1D
        fadeToBlackBy(120) 
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

    private fun lerp(start: Int, end: Int, t: Double): Double = start + (end - start) * t
    
    private fun scaleColorBrightness(color: Int, brightness: Int): Int {
        val factor = brightness / 255.0
        return Color.rgb(
            (Color.red(color) * factor).toInt(),
            (Color.green(color) * factor).toInt(),
            (Color.blue(color) * factor).toInt()
        )
    }
}
