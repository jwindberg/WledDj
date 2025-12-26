package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.LoudnessMeter

/**
 * Swirl animation - Audio-reactive swirling pixels with mirrored patterns.
 * Migrated to WledDj.
 */
class SwirlAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var custom1: Int = 16
    private var fadeAmount: Int = 4
    private var loudnessMeter: LoudnessMeter? = null
    
    private var startTimeNs: Long = 0L

    override fun onInit() {
        loudnessMeter = LoudnessMeter()
        paramIntensity = 64
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        // Get loudness
        val loudness = loudnessMeter?.getNormalizedLoudness() ?: 0
        val rawVolume = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        val smoothVolume = loudness / 1024.0f * 255.0f

        val NOISE_FLOOR = 5
        if (rawVolume < NOISE_FLOOR) {
            fadeToBlackBy(15) // WledFx: fadeToBlack(15)
            // blur skipped to save perf or if needed implement
            return true
        }

        fadeToBlackBy(fadeAmount)
        if (custom1 > 0) blur2d(custom1)

        val timeMs = (now - startTimeNs) / 1_000_000 // Ensure 0-based time
        if (startTimeNs == 0L) { startTimeNs = now; }

        var freq1 = (27 * paramSpeed) / 255
        var freq2 = (41 * paramSpeed) / 255
        if (freq1 < 1) freq1 = 1
        if (freq2 < 1) freq2 = 1

        val BORDER_WIDTH = 2
        val i = MathUtils.beatsin8(freq1, BORDER_WIDTH, width - BORDER_WIDTH, timeMs)
        val j = MathUtils.beatsin8(freq2, BORDER_WIDTH, height - BORDER_WIDTH, timeMs)
        val ni = width - 1 - i
        val nj = height - 1 - j

        val baseBrightness = 200
        val audioBoost = rawVolume / 2
        var brightness = (baseBrightness + audioBoost).coerceAtMost(255)
        brightness = brightness.coerceAtLeast(150)

        val paletteOffset = smoothVolume * 4.0f

        val color1 = colorFromPalette((timeMs / 11 + paletteOffset).toInt(), brightness)
        addPixelColor(i, j, color1)

        val color2 = colorFromPalette((timeMs / 13 + paletteOffset).toInt(), brightness)
        addPixelColor(j, i, color2)

        val color3 = colorFromPalette((timeMs / 17 + paletteOffset).toInt(), brightness)
        addPixelColor(ni, nj, color3)

        val color4 = colorFromPalette((timeMs / 29 + paletteOffset).toInt(), brightness)
        addPixelColor(nj, ni, color4)

        val color5 = colorFromPalette((timeMs / 37 + paletteOffset).toInt(), brightness)
        addPixelColor(i, nj, color5)

        val color6 = colorFromPalette((timeMs / 41 + paletteOffset).toInt(), brightness)
        addPixelColor(ni, j, color6)

        return true
    }

    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    // Additive color helper with 3x3 blob
    private fun addPixelColor(cx: Int, cy: Int, color: Int) {
        val rAdd = Color.red(color)
        val gAdd = Color.green(color)
        val bAdd = Color.blue(color)

        // Draw 3x3 blob centered at cx, cy
        for (dy in -1..1) {
            for (dx in -1..1) {
                val x = cx + dx
                val y = cy + dy
                
                if (x in 0 until width && y in 0 until height) {
                    val current = getPixelColor(x, y)
                    val r = (Color.red(current) + rAdd).coerceAtMost(255)
                    val g = (Color.green(current) + gAdd).coerceAtMost(255)
                    val b = (Color.blue(current) + bAdd).coerceAtMost(255)
                    setPixelColor(x, y, Color.rgb(r, g, b))
                }
            }
        }
    }
    
    private fun colorFromPalette(index: Int, brightness: Int): Int {
         val base = getColorFromPalette(index)
         return fadeColor2(base, 255 - brightness)
    }
    
    private fun fadeColor2(color: Int, amount: Int): Int { 
        // Helper to scale brightness by inverse amount (0=full, 255=black)
        val scale = (255 - amount).coerceAtLeast(0) / 255.0
        return Color.rgb(
           (Color.red(color) * scale).toInt(),
           (Color.green(color) * scale).toInt(),
           (Color.blue(color) * scale).toInt()
        )
    }
}
