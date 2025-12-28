package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import kotlin.math.min

/**
 * Plasmoid animation - Plasma-like effect with audio reactivity
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palettes

class PlasmoidAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var thisPhase: Int = 0
    private var thatPhase: Int = 0

    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun onInit() {
        thisPhase = 0
        thatPhase = 0
        startTimeNs = System.nanoTime()
        loudnessMeter = LoudnessMeter()
        currentPalette = Palettes.get("Rainbow")
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000

        // Fade to black by 32
        fadeToBlackBy(32)

        // Update phases
        thisPhase += MathUtils.beatsin8(6, -4, 4, timeMs)
        thatPhase += MathUtils.beatsin8(7, -4, 4, timeMs)

        val loudness = loudnessMeter?.getNormalizedLoudness() ?: 0
        val volumeScaled = (loudness / 1024.0f * 255.0f).coerceIn(0.0f, 255.0f)
        val audioModulation = (volumeScaled * paramIntensity / 255.0f).toInt().coerceIn(0, 255)

        val segmentLength = width * height

        for (i in 0 until segmentLength) {
            val x = i % width
            val y = i / width

            // cubicwave8(((i*(1 + (3*speed/32)))+thisPhase) & 0xFF)/2
            val wave1Arg = ((i * (1 + (3 * paramSpeed / 32))) + thisPhase) and 0xFF
            val thisbright = MathUtils.cubicwave8(wave1Arg) / 2

            // cos8(((i*(97 +(5*speed/32)))+thatPhase) & 0xFF)/2
            val wave2Arg = ((i * (97 + (5 * paramSpeed / 32))) + thatPhase) and 0xFF
            val brightness = thisbright + (MathUtils.cos8(wave2Arg) / 2)

            val colorIndex = brightness.coerceIn(0, 255)

            // Audio reactivity
            val finalBrightness = if (audioModulation < 10) {
                (brightness * 0.1f).toInt().coerceIn(0, 255)
            } else {
                val baseBrightness = (brightness * (0.3f + audioModulation / 255.0f * 0.7f)).toInt()
                val audioBoost = audioModulation / 2 
                (baseBrightness + audioBoost).coerceIn(0, 255)
            }

            // Pseudo palette
            val color = getColorFromPalette(colorIndex)
            // Scale by finalBrightness
            val blendedColor = fadeColor2(color, 255 - finalBrightness)
            addPixelColor(x, y, blendedColor)
        }
        return true
    }

    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
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
    
    // Helper to dim color (similar to scaleBrightness)
    private fun fadeColor2(color: Int, amount: Int): Int {
        val scale = (255 - amount).coerceAtLeast(0) / 255.0
        return Color.rgb(
           (Color.red(color) * scale).toInt(),
           (Color.green(color) * scale).toInt(),
           (Color.blue(color) * scale).toInt()
        )
    }
}
