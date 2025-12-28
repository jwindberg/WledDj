package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

/**
 * Lake - Water ripple effect
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palettes

class LakeAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var startTimeNs: Long = 0L

    override fun onInit() {
        startTimeNs = System.nanoTime()
        currentPalette = Palettes.get("Ocean")
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val sp = paramSpeed / 10
        val wave1 = beatsin8(sp + 2, -64, 64)
        val wave2 = beatsin8(sp + 1, -64, 64)
        val wave3 = beatsin8(sp + 2, 0, 80)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = x + y * width
                val index = MathUtils.cos8((i * 15) + wave1) / 2 + cubicwave8((i * 23) + wave2) / 2
                val lum = if (index > wave3) index - wave3 else 0
                
                // Color scaling
                val baseColor = getColorFromPalette(index)
                val pixelColor = if (lum > 0) {
                     scaleColorBrightness(baseColor, lum)
                } else {
                     Color.BLACK
                }
                
                setPixelColor(x, y, pixelColor)
            }
        }
        return true
    }

    private fun beatsin8(bpm: Int, low: Int, high: Int): Int {
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000
        // Use MathUtils or local implementation
        return MathUtils.beatsin8(bpm, low, high, timeMs)
    }
    
    private fun cubicwave8(angle: Int): Int {
        val x = (angle % 256) / 256.0
        val cubic = 4 * x * x * x - 6 * x * x + 3 * x
        return (cubic * 255).toInt().coerceIn(0, 255)
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
