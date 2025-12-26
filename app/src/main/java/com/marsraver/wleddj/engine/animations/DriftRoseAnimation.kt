package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.PI

/**
 * Drift Rose animation
 * Migrated to WledDj.
 */
class DriftRoseAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    // Temp buffer for blur
    private lateinit var tempBuffer: IntArray

    override fun onInit() {
        tempBuffer = IntArray(width * height)
    }

    override fun update(now: Long): Boolean {
        // BasePixel uses 1D int array `pixels` but mapped to 2D
        if (tempBuffer.size != width * height) {
             tempBuffer = IntArray(width * height)
        }
    
        val centerX = (width / 2.0) - 0.5
        val centerY = (height / 2.0) - 0.5
        val radius = min(width, height) / 2.0
        val timeSeconds = now / 1_000_000_000.0

        for (i in 1..36) {
            val angle = Math.toRadians(i * 10.0)
            val wave = beatsin8(i, 0.0, radius * 2, timeSeconds) - radius
            val x = centerX + sin(angle) * wave
            val y = centerY + cos(angle) * wave
            val hue = (i * 10) % 256
            drawPixelXYF(x, y, getColorFromPalette(hue))
        }

        fadeToBlackBy(32)
        blur2d(16)
        return true
    }

    private fun drawPixelXYF(x: Double, y: Double, rgb: Int) {
        val floorX = x.toInt()
        val floorY = y.toInt()
        val fracX = (x - floorX).coerceIn(0.0, 1.0)
        val fracY = (y - floorY).coerceIn(0.0, 1.0)
        val invX = 1.0 - fracX
        val invY = 1.0 - fracY

        val weights = listOf(invX * invY, fracX * invY, invX * fracY, fracX * fracY)
        val offsets = listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1)

        for ((index, weight) in weights.withIndex()) {
            val (dx, dy) = offsets[index]
            val xx = floorX + dx
            val yy = floorY + dy
            if (xx !in 0 until width || yy !in 0 until height) continue

            val current = getPixelColor(xx, yy)
            // Additive blending with weight
            val r = (Color.red(current) + Color.red(rgb) * weight).roundToInt().coerceAtMost(255)
            val g = (Color.green(current) + Color.green(rgb) * weight).roundToInt().coerceAtMost(255)
            val b = (Color.blue(current) + Color.blue(rgb) * weight).roundToInt().coerceAtMost(255)
            
            setPixelColor(xx, yy, Color.rgb(r, g, b))
        }
    }

    private fun beatsin8(speed: Int, minValue: Double, maxValue: Double, timeSeconds: Double): Double {
        val amplitude = (maxValue - minValue) / 2.0
        val mid = minValue + amplitude
        val frequency = speed / 16.0
        val sine = sin(2 * PI * frequency * timeSeconds)
        return mid + sine * amplitude
    }
}
