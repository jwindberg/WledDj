package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils

/**
 * Distortion Waves animation
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palette

class DistortionWavesAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true
    
    private var scale: Int = 64
    private var zoom: Boolean = false
    private var alt: Boolean = false
    private var paletteMode: Int = 1 // default to 1 (HSV/Palette)
    private var startTimeMs: Long = 0
    
    override fun getDefaultPalette(): Palette = Palette.RAINBOW

    override fun onInit() {
        startTimeMs = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        // BasePixel uses now in Nano. 
        // Logic relies on Millis.
        val timeMs = now / 1_000_000
        if (startTimeMs == 0L) startTimeMs = timeMs 
        
        val elapsed = timeMs - startTimeMs

        // We iterate every pixel and set color
        val speedVal = paramSpeed / 32
        var scaleVal = scale / 32

        if (zoom) scaleVal += 192 / (width + height)

        val a = elapsed / 32
        val a2 = a / 2
        val a3 = a / 3

        val colsScaled = width * scaleVal
        val rowsScaled = height * scaleVal

        // beatsin8 returns int 0-255ish but mapped to range.
        val cx = MathUtils.beatsin8(10 - speedVal, 0, colsScaled, elapsed)
        val cy = MathUtils.beatsin8(12 - speedVal, 0, rowsScaled, elapsed)
        val cx1 = MathUtils.beatsin8(13 - speedVal, 0, colsScaled, elapsed)
        val cy1 = MathUtils.beatsin8(15 - speedVal, 0, rowsScaled, elapsed)
        val cx2 = MathUtils.beatsin8(17 - speedVal, 0, colsScaled, elapsed)
        val cy2 = MathUtils.beatsin8(14 - speedVal, 0, rowsScaled, elapsed)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val xoffs = x * scaleVal
                val yoffs = y * scaleVal

                val rdistort: Int
                val gdistort: Int
                val bdistort: Int

                // MathUtils.cos8
                if (alt) {
                    rdistort = MathUtils.cos8(((x + y) * 8 + a.toInt()) and 255) shr 1
                    gdistort = MathUtils.cos8(((x + y) * 8 + a3.toInt() + 32) and 255) shr 1
                    bdistort = MathUtils.cos8(((x + y) * 8 + a2.toInt() + 64) and 255) shr 1
                } else {
                    val termR = (MathUtils.cos8(((x shl 3) + a.toInt()) and 255)
                            + MathUtils.cos8(((y shl 3) - a2.toInt()) and 255)
                            + a3.toInt()) and 255
                    val termG = (MathUtils.cos8(((x shl 3) - a2.toInt()) and 255)
                            + MathUtils.cos8(((y shl 3) + a3.toInt()) and 255)
                            + a.toInt() + 32) and 255
                    val termB = (MathUtils.cos8(((x shl 3) + a3.toInt()) and 255)
                            + MathUtils.cos8(((y shl 3) - a.toInt()) and 255)
                            + a2.toInt() + 64) and 255

                    rdistort = MathUtils.cos8(termR) shr 1
                    gdistort = MathUtils.cos8(termG) shr 1
                    bdistort = MathUtils.cos8(termB) shr 1
                }

                val distR = ((xoffs - cx) * (xoffs - cx) + (yoffs - cy) * (yoffs - cy)) shr 7
                val distG = ((xoffs - cx1) * (xoffs - cx1) + (yoffs - cy1) * (yoffs - cy1)) shr 7
                val distB = ((xoffs - cx2) * (xoffs - cx2) + (yoffs - cy2) * (yoffs - cy2)) shr 7

                var valueR = rdistort + (((a - distR).toInt()) shl 1)
                var valueG = gdistort + (((a2 - distG).toInt()) shl 1)
                var valueB = bdistort + (((a3 - distB).toInt()) shl 1)

                valueR = MathUtils.cos8(valueR and 255)
                valueG = MathUtils.cos8(valueG and 255)
                valueB = MathUtils.cos8(valueB and 255)

                if (paletteMode == 0) {
                     setPixelColor(x, y, Color.rgb(valueR, valueG, valueB))
                } else {
                    // Use H from RGB
                    // Simplified: just map avg brightness to hue? Or H from RGB conversion?
                    // Source: val hsv = ... rgbToHsv ... hue -> colorFromPalette
                    // Let's approximate: 
                    // Or just use valueR as hue?
                    // Let's use valueR as hue index
                    val hue = valueR
                    setPixelColor(x, y, getColorFromPalette(hue))
                }
            }
        }
        return true
    }
}
