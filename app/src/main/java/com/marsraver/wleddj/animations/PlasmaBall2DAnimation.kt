package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.min

/**
 * PlasmaBall2D - 2D plasma ball effect
 * Migrated to WledDj.
 */
class PlasmaBall2DAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var startTimeNs: Long = 0L
    private var fadeAmount: Int = 64
    private var blurAmount: Int = 4

    override fun onInit() {
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = System.nanoTime()

        // Fade to black
        fadeToBlackBy(fadeAmount)

        val speedDiv = (256 - paramSpeed).coerceAtLeast(1)
        val t = ((System.nanoTime() - startTimeNs) / 1_000_000 * 8) / speedDiv

        for (i in 0 until width) {
            val thisVal = MathUtils.inoise8(i * 30, t.toInt(), t.toInt())
            val thisMax = MathUtils.map(thisVal, 0, 255, 0, width - 1)

            for (j in 0 until height) {
                val thisVal_ = MathUtils.inoise8(t.toInt(), j * 30, t.toInt())
                val thisMax_ = MathUtils.map(thisVal_, 0, 255, 0, height - 1)

                val x = (i + thisMax_ - width / 2)
                val y = (j + thisMax - width / 2)
                val cx = (i + thisMax_)
                val cy = (j + thisMax)

                val shouldDraw = ((x - y > -2) && (x - y < 2)) ||
                                ((width - 1 - x - y) > -2 && (width - 1 - x - y < 2)) ||
                                (width - cx == 0) ||
                                (width - 1 - cx == 0) ||
                                (height - cy == 0) ||
                                (height - 1 - cy == 0)

                if (shouldDraw) {
                    val beat = beat8(5)
                    // Palette color
                    val baseColor = getColorFromPalette(beat)
                    val color = fadeColor(baseColor, 255 - thisVal)
                    addPixelColor(i, j, color)
                }
            }
        }

        // Apply blur (Not implemented in base fully yet, but we'll use a placeholder or add if needed)
        // With fadeToBlackBy, blur provides trails.
        // For now, skip explicit blur or implement simple one
        // blur(blurAmount) ?

        return true
    }
    
    // Additive color 
    private fun addPixelColor(x: Int, y: Int, color: Int) {
         if (x in 0 until width && y in 0 until height) {
             val current = getPixelColor(x, y)
             val r = min(Color.red(current) + Color.red(color), 255)
             val g = min(Color.green(current) + Color.green(color), 255)
             val b = min(Color.blue(current) + Color.blue(color), 255)
             setPixelColor(x, y, Color.rgb(r, g, b))
         }
    }
    
    private fun beat8(bpm: Int): Int {
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000
        return ((timeMs * bpm / 60000.0 * 256.0) % 256).toInt()
    }
}
