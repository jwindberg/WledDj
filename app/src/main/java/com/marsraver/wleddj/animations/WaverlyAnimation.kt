package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Waverly animation - mirrored rainbow columns with noise
 * Migrated to WledDj.
 */
class WaverlyAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var timeValue: Double = 0.0
    private var loudnessMeter: LoudnessMeter? = null
    private var lastUpdateNs: Long = 0L

    // In BasePixelAnimation, we already have width/height/pixels
    
    override fun onInit() {
        timeValue = 0.0
        lastUpdateNs = System.nanoTime()
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
            return true
        }
        val deltaMs = (now - lastUpdateNs) / 1_000_000.0
        lastUpdateNs = now

        val speedFactor = paramSpeed / 128.0
        timeValue += (deltaMs * 0.5 * speedFactor)
        
        // Clear frame
        for (i in 0 until width*height) pixels[i] = Color.BLACK

        val loudness = loudnessMeter?.getNormalizedLoudness() ?: 0
        val level = (loudness / 1024.0 * 255.0).coerceIn(0.0, 255.0)

        if (level <= 0.0) return true

        val levelFactor = level / 255.0
        val heightMultiplier = levelFactor * 3.5 
        val brightnessBase = (levelFactor * 255.0).coerceIn(0.0, 255.0)
        val blurAmount = (levelFactor * 127.0).roundToInt().coerceIn(0, 127)

        for (i in 0 until width) {
            val noiseVal = MathUtils.inoise8(i * 45, timeValue.toInt(), (timeValue * 0.6).toInt())
            // inoise8 returns 0-255.
            
            // Map 0-255 to 0-height
            val baseHeight = (noiseVal / 255.0 * height)
            val thisMax = (baseHeight * heightMultiplier).roundToInt().coerceIn(0, height)
            
            if (thisMax <= 0) continue

            val minBrightness = 180 
            val brightness = (minBrightness + (brightnessBase - minBrightness) * 0.3).roundToInt().coerceIn(180, 255)
            
            for (j in 0 until thisMax) {
                // Hue mapping: 0 to thisMax -> 250 to 0 (rainbow)
                // Hue mapping: 0 to thisMax -> 250 to 0 (rainbow)
                val hue = MathUtils.map(j, 0, thisMax, 250, 0)
                val color = fadeColor(getColorFromPalette(hue), 255 - brightness)
                
                addPixelColor(i, j, color)
                val mirrorX = width - 1 - i
                val mirrorY = height - 1 - j
                addPixelColor(mirrorX, mirrorY, color)
            }
        }

        if (blurAmount > 0) blur2d(blurAmount)
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
}
