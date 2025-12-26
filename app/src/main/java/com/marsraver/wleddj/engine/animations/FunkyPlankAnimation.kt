package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.FftMeter

/**
 * Funky Plank animation - 2D scrolling FFT visualization.
 * Migrated to WledDj.
 */
class FunkyPlankAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var fftMeter: FftMeter? = null
    private var lastSecondHand: Int = -1
    private var startTimeNs: Long = 0L
    private var noiseThreshold: Int = 10 

    override fun onInit() {
        lastSecondHand = -1
        startTimeNs = System.nanoTime()
        fftMeter = FftMeter(bands = 16)
        // Default paramSpeed/Intensity handled by Base
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        // Map custom1 (assuming standard params, but user code had custom1)
        // We'll use paramIntensity to map number of bands? Or just fixed?
        // Original: map(custom1, 0, 255, 1, 16)
        // Let's use 255 (full) as default or map paramIntensity? 
        // Let's map paramIntensity to number of bands used for color cycling.
        val numBands = map(paramIntensity, 0, 255, 1, 16).coerceIn(1, 16)

        val micros = (now - startTimeNs) / 1_000L
        val speedDivisor = (256 - paramSpeed).coerceAtLeast(1)
        val secondHand = ((micros / speedDivisor / 500 + 1) % 64).toInt()

        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand
            val fftBands = fftMeter?.getNormalizedBands() ?: IntArray(16)

            for (x in 0 until width) {
                // We have 16 bands. Map x to bands.
                val bandIndex = x % 16
                val fftValue = fftBands.getOrElse(bandIndex) { 0 }
                
                val rgb = if (fftValue < noiseThreshold) {
                    Color.BLACK
                } else {
                    val colorIndex = x % numBands
                    // Map to 0-255 for palette
                    val paletteIdx = (colorIndex * 255 / numBands.coerceAtLeast(1))
                    val baseColor = getColorFromPalette(paletteIdx)
                    
                    val brightness = map(fftValue, noiseThreshold, 255, 10, 255).coerceIn(10, 255)
                    
                    // Scale brightness
                    fadeColor(baseColor, 255 - brightness)
                }
                setPixelColor(x, 0, rgb)
            }
        }

        // Scroll up
        for (i in height - 1 downTo 1) {
            for (j in 0 until width) {
                setPixelColor(j, i, getPixelColor(j, i - 1))
            }
        }
        return true
    }

    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).toInt().coerceIn(toLow, toHigh)
    }
    
    private fun scaleColorBrightness(color: Int, brightness: Int): Int {
        val factor = brightness / 255.0
        return Color.rgb(
            (Color.red(color) * factor).toInt(),
            (Color.green(color) * factor).toInt(),
            (Color.blue(color) * factor).toInt()
        )
    }
}
