package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import kotlin.math.sqrt
import kotlin.math.roundToInt

/**
 * PixelWave animation - Wave expanding from center based on audio
 * Migrated to WledDj.
 */
class PixelWaveAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var secondHand: Int = 0
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun onInit() {
        secondHand = 0
        startTimeNs = System.nanoTime()
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeMicros = (now - startTimeNs) / 1_000

        // Fade out
        fadeToBlackBy(15) // WledFx fadeOut(240) -> 255-240 = 15

        val speedFactor = (256 - paramSpeed).coerceAtLeast(1)
        val newSecondHand = ((timeMicros / speedFactor / 500 + 1) % 16).toInt()

        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val volume = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)

        if (newSecondHand != secondHand) {
            secondHand = newSecondHand

            val pixBri = ((volume * paramIntensity / 64) + 50).coerceIn(50, 255)

            val colorIndex = (timeMs % 256).toInt()
            val color = fadeColor(getColorFromPalette(colorIndex), 255 - pixBri)

            val centerX = width / 2
            val centerY = height / 2
            setPixelColor(centerX, centerY, color)
        }

        expandWaveFromCenter()
        return true
    }

    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    private fun expandWaveFromCenter() {
        val centerX = width / 2
        val centerY = height / 2
        
        // Copy current frame to temp buffer to simulate wave propagation
        // BasePixelAnimation pixels is current frame.
        val temp = pixels.clone()

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (x == centerX && y == centerY) continue

                val dx = x - centerX
                val dy = y - centerY
                val distance = sqrt((dx * dx + dy * dy).toDouble())

                if (distance < 0.5) continue

                // Pull pixel from closer to center (reverse expansion)
                val stepSize = 1.0 / distance.coerceAtLeast(1.0)
                val sourceX = centerX + (dx * (1.0 - stepSize)).roundToInt()
                val sourceY = centerY + (dy * (1.0 - stepSize)).roundToInt()

                val sourceColor = if (sourceX in 0 until width && sourceY in 0 until height) {
                    temp[sourceY * width + sourceX]
                } else {
                    Color.BLACK
                }
                setPixelColor(x, y, sourceColor)
            }
        }
    }
}
