package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.math.max

/**
 * GEQ (Graphic Equalizer) animation - 2D frequency band visualization.
 * Adapted for Android Canvas.
 */
class GeqAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var fftMeter: FftMeter? = null
    private var previousBarHeight: IntArray = IntArray(0)
    
    private var lastRippleTime: Long = 0
    private var callCount: Long = 0
    
    // Config
    private var numBands: Int = 16
    private var noiseFloor: Int = 20
    
    init {
        // Enforce 32x32 grid to match original look
        init(32, 32)
    }

    override fun onInit() {
        previousBarHeight = IntArray(width)
        lastRippleTime = 0
        callCount = 0
        // Use 256 bands FFT from AudioPipeline setup
        fftMeter = FftMeter(bands = 256)
    }

    override fun update(now: Long): Boolean {
        callCount++
        
        val fullSpectrum = fftMeter?.getNormalizedBands() ?: IntArray(width)
        
        var rippleTime = false
        var rippleInterval = 256 - paramIntensity
        if (rippleInterval < 1) rippleInterval = 1
        
        // 128 ~ 30ms heuristic
        if (now - lastRippleTime >= rippleInterval * 500_000L) {
            lastRippleTime = now
            rippleTime = true
        }

        // Fade Logic
        fadeToBlackBy(paramSpeed / 10)

        for (x in 0 until width) {
            // Map X column (0-31) to Frequency Band index (0-255)
            // Low frequencies are usually more interesting.
            // Let's grab a range 0..80 to focus on Bass/Mids
            val bandIndex = mapValue(x, 0, width, 0, 80)
            
            // Get FFT Value
            val rawValue = fullSpectrum.getOrElse(bandIndex) { 0 }
            val effectiveValue = max(0, rawValue - noiseFloor)
            
            // Map FFT value to Height (0-32)
            var barHeight = mapValue(effectiveValue, 0, 255 - noiseFloor, 0, height)
            barHeight = barHeight.coerceIn(0, height)

            // Peak Hold
            if (barHeight > previousBarHeight[x]) {
                previousBarHeight[x] = barHeight
            }

            // Draw Bar
            // Color based on Palette
            // Map x to palette index
            val colorIndex = (x * 8).coerceIn(0, 255)
            val ledColor = getColorFromPalette(colorIndex)

            for (y in 0 until barHeight) {
                // y=0 is bottom
                setPixelColor(x, height - 1 - y, ledColor)
            }

            // Draw Peak Dot (White)
            if (previousBarHeight[x] > 0) {
                 val peakY = (height - previousBarHeight[x]).coerceIn(0, height - 1)
                 setPixelColor(x, peakY, Color.WHITE)
            }

            // Gravity for Peak
            if (rippleTime && previousBarHeight[x] > 0) {
                previousBarHeight[x]--
            }
        }
        return true
    }

    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
    }

    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
