package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Sun Radiation animation - heat/palette driven bump-mapped plasma.
 * Migrated to WledDj.
 */
class SunRadiationAnimation : BasePixelAnimation() {

    private lateinit var bump: IntArray
    private lateinit var bumpTemp: IntArray
    // We'll generate LUT on fly or use a simple map
    private var lut = IntArray(256)
    private var lutGenerated = false
    private var lastUpdateNanos: Long = 0L

    override fun onInit() {
        bump = IntArray((width + 2) * (height + 2))
        bumpTemp = IntArray(bump.size)
        lutGenerated = false
        lastUpdateNanos = 0L
    }

    override fun update(now: Long): Boolean {
        if (!lutGenerated) {
            generateLut()
            lutGenerated = true
        }

        val deltaMs = if (lastUpdateNanos == 0L) 0.0 else (now - lastUpdateNanos) / 1_000_000.0
        lastUpdateNanos = now
        val timeSeconds = now / 1_000_000_000.0

        generateBump(timeSeconds, deltaMs)
        bumpMap()
        return true
    }
    
    // Using default heat color map since we do not fully support custom palettes yet
    private fun generateLut() {
        for (i in 0 until 256) {
             // heatColor logic: 
             // scale 0-255 -> heat color (Black -> Red -> Yellow -> White)
             // We can use built-in or map manually
             lut[i] = heatColor((i / 1.4).roundToInt().coerceIn(0, 255))
        }
    }
    
    private fun heatColor(temperature: Int): Int {
        val t = temperature.coerceIn(0, 255)
        // Scale 'heat' down to 0-255
        // 0 -> Black
        // 85 -> Red (255, 0, 0)
        // 170 -> Yellow (255, 255, 0)
        // 255 -> White (255, 255, 255)
        
        var r = 0; var g = 0; var b = 0
        
        // Scale t to 0-765 (3 * 255) area? 
        // Wled/FastLED HeatColor logic:
        /*
          // Scale 'heat' down from 0-255 to 0-191,
          // which can then be easily divided into three
          // equal 'thirds' of 64 units each.
          byte t192 = scale8_video( temperature, 192);
          
          byte heatramp = t192 & 0x3F; // 0..63
          heatramp <<= 2; // scale up to 0..252

          if( t192 & 0x80) {
            // hottest
            r = 255; g = 255; b = heatramp;
          } else if( t192 & 0x40 ) {
            // middle
            r = 255; g = heatramp; b = 0;
          } else {
            // coolest
            r = heatramp; g = 0; b = 0;
          }
        */
        
        // Simplified impl:
        if (t < 85) {
            r = t * 3
            g = 0
            b = 0
        } else if (t < 170) {
            r = 255
            g = (t - 85) * 3
            b = 0
        } else {
            r = 255
            g = 255
            b = (t - 170) * 3
        }
        return Color.rgb(r, g, b)
    }

    private fun generateBump(timeSeconds: Double, deltaMs: Double) {
        val w = width + 2
        val h = height + 2
        val z = timeSeconds * 0.3
        var index = 0
        for (j in 0 until h) {
            for (i in 0 until w) {
                // Use MathUtils.perlinNoise if available, simplified here or use inoise8
                // MathUtils.inoise8 returns 0-255.
                // We need float 3d noise perhaps?
                // The source used MathUtils.perlinNoise(double, double, double).
                // Let's assume MathUtils.inoise8 works similarly but scaled 0-255 int.
                // Double size -> Half frequency defaults
                // Was 50, now 25
                val val8 = MathUtils.inoise8( (i*25), (j*25), (z*255).toInt())
                bump[index++] = val8 / 2
            }
        }
        smoothBump(w, h)
    }

    private fun bumpMap() {
        val extendedWidth = width + 2
        var yIndex = extendedWidth + 1
        var vly = -(height / 2 + 1)

        for (y in 0 until height) {
            vly++
            var vlx = -(width / 2 + 1)
            for (x in 0 until width) {
                vlx++
                val nx = bump[yIndex + x + 1] - bump[yIndex + x - 1]
                val ny = bump[yIndex + x + extendedWidth] - bump[yIndex + x - extendedWidth]
                // Was 7, now 3 (approx double size / half coordinate density)
                val difx = abs(vlx * 3 - nx)
                val dify = abs(vly * 3 - ny)
                val temp = difx * difx + dify * dify
                var col = 255 - temp / 12
                if (col < 0) col = 0
                val color = lut[col.coerceIn(0, 255)]
                setPixelColor(x, y, color)
            }
            yIndex += extendedWidth
        }
    }

    private fun smoothBump(w: Int, h: Int) {
        for (j in 1 until h - 1) {
            for (i in 1 until w - 1) {
                val idx = i + j * w
                var sum = 0
                sum += bump[idx]
                sum += bump[idx - 1]
                sum += bump[idx + 1]
                sum += bump[idx - w]
                sum += bump[idx + w]
                sum += bump[idx - w - 1]
                sum += bump[idx - w + 1]
                sum += bump[idx + w - 1]
                sum += bump[idx + w + 1]
                bumpTemp[idx] = sum / 9
            }
        }
        for (j in 1 until h - 1) {
            for (i in 1 until w - 1) {
                val idx = i + j * w
                bump[idx] = bumpTemp[idx]
            }
        }
    }
}
