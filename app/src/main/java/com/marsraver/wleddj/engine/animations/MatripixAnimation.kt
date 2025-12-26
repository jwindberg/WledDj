package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.LoudnessMeter

/**
 * Matripix animation - Shifting pixels with audio-reactive brightness
 * Migrated to WledDj.
 */
class MatripixAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var secondHand: Int = 0
    private var lastSecondHand: Int = -1
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun onInit() {
        secondHand = 0
        lastSecondHand = -1
        startTimeNs = System.nanoTime()
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeMicros = (now - startTimeNs) / 1_000
        
        val speedFactor = (256 - paramSpeed).coerceAtLeast(1)
        secondHand = ((timeMicros / speedFactor / 500) % 16).toInt()
        
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val volume = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        
        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand
            
            val pixBri = (volume * paramIntensity / 64).coerceIn(0, 255)
            
            val colorIndex = (timeMs % 256).toInt()
            val newColor = getColorFromPalette(colorIndex) // Base color
            
            for (y in 0 until height) {
                // Shift left
                for (x in 0 until width - 1) {
                    setPixelColor(x, y, getPixelColor(x + 1, y))
                }
                
                // Add new pixel
                // Blend black with newColor amount pixBri
                val r = (Color.red(newColor) * pixBri / 255)
                val g = (Color.green(newColor) * pixBri / 255)
                val b = (Color.blue(newColor) * pixBri / 255)
                setPixelColor(width - 1, y, Color.rgb(r, g, b))
            }
        }
        
        return true
    }
    
    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }
}
