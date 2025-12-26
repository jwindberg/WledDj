package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

/**
 * Waving Cell animation - heat palette waves animated with sinusoidal motion.
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palettes

class WavingCellAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var timeValue: Double = 0.0

    override fun onInit() {
        timeValue = 0.0
        currentPalette = Palettes.get("Heat")
    }

    override fun update(now: Long): Boolean {
        timeValue = now / 1_000_000.0 / 100.0
        
        val energy = 1.0
        val brightnessScale = 1.0

        val t = timeValue
        
        for (y in 0 until height) {
             val inner = sin8(y * 5.0 + t * 5.0 * energy)
             val vertical = cos8(y * 10.0 * energy)
             
             for (x in 0 until width) {
                  val wave = sin8(x * 10.0 + inner * energy)
                  
                  // Index for heat palette
                  var index = wave * energy + vertical * (0.7 + energy * 0.3) + t
                  index = wrapToPaletteRange(index)
                  
                  // Heat palette simulation
                  val color = getColorFromPalette(index.toInt().coerceIn(0, 255))
                  
                  // Brightness scale?
                  // Just set
                  setPixelColor(x, y, color)
             }
        }
        return true
    }

    private fun heatColor(v: Int): Int {
        // Simple HeatColor approx
        // 0..255
        // 0-85: Red increases
        // 85-170: Green increases (Yellow)
        // 170-255: Blue increases (White)
        
        var r = 0; var g = 0; var b = 0
        if (v < 85) {
            r = v * 3
        } else if (v < 170) {
            r = 255
            g = (v - 85) * 3
        } else {
            r = 255
            g = 255
            b = (v - 170) * 3
        }
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun sin8(theta: Double): Double {
        var angle = theta % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * PI
        return (sin(radians) + 1.0) * 127.5
    }

    private fun cos8(theta: Double): Double {
        var angle = theta % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * PI
        return (cos(radians) + 1.0) * 127.5
    }

    private fun wrapToPaletteRange(value: Double): Double {
        var result = value % 256.0
        if (result < 0) result += 256.0
        return result
    }
}
