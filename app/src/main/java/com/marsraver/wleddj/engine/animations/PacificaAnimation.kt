package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils

/**
 * Pacifica - Peaceful ocean waves
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palettes

class PacificaAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var startTimeNs: Long = 0L
    private var sCIStart1: Int = 0
    private var sCIStart2: Int = 0
    private var sCIStart3: Int = 0
    private var sCIStart4: Int = 0

    override fun onInit() {
        startTimeNs = System.nanoTime()
        currentPalette = Palettes.get("Ocean")
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = System.nanoTime()

        // Slow down time for calmer waves
        val timeMs = (System.nanoTime() - startTimeNs) / 3_000_000
        // Reduce delta speed
        val deltams = 5 + (5 * paramSpeed / 128)

        // Update wave counters
        val speedfactor1 = MathUtils.beatsin16(3, 179, 269, timeMs)
        val speedfactor2 = MathUtils.beatsin16(4, 179, 269, timeMs)
        val deltams1 = (deltams * speedfactor1) / 256
        val deltams2 = (deltams * speedfactor2) / 256
        val deltams21 = (deltams1 + deltams2) / 2

        sCIStart1 += (deltams1 * MathUtils.beatsin16(1011, 10, 13, timeMs)) / 256
        sCIStart2 -= (deltams21 * MathUtils.beatsin16(777, 8, 11, timeMs)) / 256
        sCIStart3 -= (deltams1 * MathUtils.beatsin16(501, 5, 7, timeMs)) / 256
        sCIStart4 -= (deltams2 * MathUtils.beatsin16(257, 4, 6, timeMs)) / 256

        val basethreshold = MathUtils.beatsin8(9, 55, 65, timeMs)
        var wave = MathUtils.beat8(7, timeMs)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = x + y * width

                var r = 2
                var g = 6
                var b = 10

                val layer1 = pacificaOneLayer(i, sCIStart1, MathUtils.beatsin16(3, 11 * 256, 14 * 256, timeMs), MathUtils.beatsin8(10, 70, 130, timeMs))
                val layer2 = pacificaOneLayer(i, sCIStart2, MathUtils.beatsin16(4, 6 * 256, 9 * 256, timeMs), MathUtils.beatsin8(17, 40, 80, timeMs))
                val layer3 = pacificaOneLayer(i, sCIStart3, 6 * 256, MathUtils.beatsin8(9, 10, 38, timeMs))
                val layer4 = pacificaOneLayer(i, sCIStart4, 5 * 256, MathUtils.beatsin8(8, 10, 28, timeMs))

                r += Color.red(layer1) + Color.red(layer2) + Color.red(layer3) + Color.red(layer4)
                g += Color.green(layer1) + Color.green(layer2) + Color.green(layer3) + Color.green(layer4)
                b += Color.blue(layer1) + Color.blue(layer2) + Color.blue(layer3) + Color.blue(layer4)

                val threshold = MathUtils.scale8(MathUtils.sin8(wave), 20) + basethreshold
                wave += 7

                val avgLight = (r + g + b) / 3
                if (avgLight > threshold) {
                    val overage = avgLight - threshold
                    val overage2 = MathUtils.qadd8(overage, overage)
                    r += overage
                    g += overage2
                    b += MathUtils.qadd8(overage2, overage2)
                }

                b = MathUtils.scale8(b, 145)
                g = MathUtils.scale8(g, 200)
                r += 2
                g += 5
                b += 7

                setPixelColor(x, y, Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255)))
            }
        }
        return true
    }

    private fun pacificaOneLayer(i: Int, cistart: Int, wavescale: Int, bri: Int): Int {
        val ci = cistart + (i * wavescale)
        val svalue8 = MathUtils.sin8((ci shr 8) and 0xFF)
        val bvalue8 = MathUtils.scale8(svalue8, bri)

        val hue = (ci shr 8) and 0xFF
        // Map hue to ocean colors
        // Standard WLED pacifica uses special palette, but hue map is:
        // hue/4 + 128 -> (32..96)+128 -> 160..224 (Blues/Cyans range in HSV)
        // With FastLED HSV, 160 is Aqua, 192 is Blue.
        // Android HSV: 0-360. 
        // (hue/4 + 128) is 0-255 based.
        // ((hue/4 + 128) / 255.0 * 360.0)
        
        // Use full hue range mapped to palette
        val color = getColorFromPalette(hue)
        
        // We need to apply 'bvalue8' (brightness/alpha) to this color
        // Original: hsvToRgb(hsvHue, 200, bvalue8) -> Saturation 200, Value bvalue8
        // Here we have RGB from palette. We should scale it by bvalue8/255.
        // And maybe desaturate slightly if we want to match '200' saturation? 
        // For simplicity, just scale brightness.
        
        return scaleColorBrightness(color, bvalue8)
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
