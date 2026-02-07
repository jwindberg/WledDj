package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.FftMeter
import com.marsraver.wleddj.engine.audio.LoudnessMeter

/**
 * FreqMatrix - 2D Frequency Matrix visualization
 * Migrated to WledDj.
 */
class FreqMatrixAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var startTimeMs: Long = 0L
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    private var lastSecondHand: Int = -1
    
    // Custom params placeholders (using defaults)
    private var custom1: Int = 0  
    private var custom2: Int = 31 
    private var custom3: Int = 10 
    
    private val MAX_FREQUENCY = 11025.0f

    override fun onInit() {
        startTimeMs = System.currentTimeMillis()
        // Use 256 bands to get ~86Hz resolution per band (44100/2/256)
        // This allows distinguishing >80Hz frequencies.
        fftMeter = FftMeter(bands = 256)
        loudnessMeter = LoudnessMeter()
        lastSecondHand = -1
    }

    override fun update(now: Long): Boolean {
        if (startTimeMs == 0L) startTimeMs = now
        // now is Millis. Convert delta to Micros.
        val micros = (now - startTimeMs) * 1_000L
        val secondHand = ((micros / (256 - paramSpeed).coerceAtLeast(1) / 500) % 16).toInt()
        
        if (lastSecondHand != secondHand) {
            lastSecondHand = secondHand
            
            var fftMajorPeak = fftMeter?.getMajorPeakFrequency() ?: 1.0f
            
            // Reverted to usage of LoudnessMeter as Original Source (No Cheating!)
            // Using Normalized Loudness to ensure it works across different device mic sensitivities.
            val volumeSmth = (loudnessMeter?.getNormalizedLoudness() ?: 0) / 1024.0f * 255.0f
            
            val sensitivity = mapValue(custom3, 0, 31, 1, 10)
            var pixVal = (volumeSmth * paramIntensity * sensitivity) / 256.0f
            if (pixVal > 255) pixVal = 255.0f
            
            val intensityValue = mapValue(pixVal.toInt(), 0, 255, 0, 100) / 100.0f
            var color: Int
            
            if (fftMajorPeak > MAX_FREQUENCY) fftMajorPeak = 1.0f
            
            if (fftMajorPeak < 20) {
                // Only black out if truly NO frequency signal (e.g. 0-20Hz noise floor?)
                // Actually, let's allow Deep Bass visualization.
                color = Color.BLACK 
            } else {
                val upperLimit = 80 + 42 * custom2
                val lowerLimit = 20 + 3 * custom1 // Relaxed lower limit
                val hue = if (lowerLimit != upperLimit) {
                    mapValue(fftMajorPeak.toInt(), lowerLimit, upperLimit, 0, 255)
                } else {
                    fftMajorPeak.toInt() and 0xFF
                }
                val brightness = (255 * intensityValue).toInt().coerceIn(0, 255)
                color = fadeColor(getColorFromPalette(hue), 255 - brightness)
            }
            
            // Shift down (using y=0 as bottom usually, but WledFx loop: 
            // for y in height-1 downTo 1... pixels[x][y] = pixels[x][y-1].
            // This shifts UP visually (y=0 moves to y=1).
            // Then sets pixels[x][0] which is bottom row.
            
            for (x in 0 until width) {
                for (y in height - 1 downTo 1) {
                    setPixelColor(x, y, getPixelColor(x, y - 1))
                }
                setPixelColor(x, 0, color)
            }
        }
        return true
    }

    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
        loudnessMeter?.stop()
        loudnessMeter = null
    }
    
    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
