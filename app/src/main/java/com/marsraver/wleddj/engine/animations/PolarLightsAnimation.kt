package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Polar Lights animation - Aurora-like effect using Perlin noise
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palettes

class PolarLightsAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var flipPalette: Boolean = false
    private var step: Long = 0L
    private var startTimeNs: Long = 0L

    override fun onInit() {
        step = 0L
        startTimeNs = System.nanoTime()
        currentPalette = Palettes.get("Forest")
    }

    override fun update(now: Long): Boolean {
        // adjustHeight map(height, 8, 32, 28, 12)
        val adjustHeight = MathUtils.map(height, 8, 32, 28, 12).toFloat() // simplified map

        // adjScale map(width, 8, 64, 310, 63)
        val adjScale = MathUtils.map(width, 8, 64, 310, 63)

        // _scale map(intensity, 0, 255, 30, adjScale)
        val _scale = MathUtils.map(paramIntensity, 0, 255, 30, adjScale)

        // _speed map(speed, 0, 255, 128, 16)
        val _speed = MathUtils.map(paramSpeed, 0, 255, 128, 16)

        // Time-based step to slow down frantic jitter (approx 15fps)
        val timeMs = (now - startTimeNs) / 1_000_000
        val step = timeMs / 60 

        for (x in 0 until width) {
            for (y in 0 until height) {
                
                val noiseX = (step % 2) + x * _scale
                val noiseY = y * 16 + (step % 16).toInt()
                val noiseZ = (step / _speed).toInt()

                // MathUtils.inoise8 takes (x, y, z)
                val perlinVal = MathUtils.inoise8(noiseX.toInt(), noiseY, noiseZ)

                val centerY = height / 2.0f
                val distanceFromCenter = abs(centerY - y.toFloat())
                val heightAdjustment = distanceFromCenter * adjustHeight

                // qsub8
                val palindex = MathUtils.qsub8(perlinVal, heightAdjustment.roundToInt())

                val finalPalIndex = if (flipPalette) 255 - palindex else palindex
                val palbrightness = palindex

                // Color from "palette". Use HSV rainbow fallback
                val color = getColorFromPalette(finalPalIndex)
                val finalColor = fadeColor2(color, 255 - palbrightness)
                setPixelColor(x, y, finalColor)
            }
        }
        return true
    }
    
    private fun fadeColor2(color: Int, amount: Int): Int {
        val scale = (255 - amount).coerceAtLeast(0) / 255.0
        return Color.rgb(
           (Color.red(color) * scale).toInt(),
           (Color.green(color) * scale).toInt(),
           (Color.blue(color) * scale).toInt()
        )
    }
}
