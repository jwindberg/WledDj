package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.math.NoiseUtils
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Washing Machine animation - Rotating waves forward, then pause, then backward.
 * Migrated to WledDj.
 */
class WashingMachineAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var step: Long = 0L
    private var startTimeNs: Long = 0L

    override fun onInit() {
        step = 0L
        startTimeNs = 0L
        // Defaults in WledFx were checked; we rely on paramSpeed/paramIntensity being set by VM
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) {
            startTimeNs = now
            return true
        }

        // Calculate speed using tristate_square8
        val timeMs = (now - startTimeNs) / 1_000_000L
        val tristateSpeed = tristateSquare8(timeMs shr 7, 90, 15)
        
        // Lower speed value means slower
        val speedFactor = (256 - paramSpeed).coerceAtLeast(1)
        step += (tristateSpeed * 128L) / speedFactor

        val segmentLength = width * height
        val intensityFactor = (paramIntensity / 25 + 1)

        for (i in 0 until segmentLength) {
             val phase = (intensityFactor * 255 * i / segmentLength) + (step shr 7).toInt()
             
             // Mapping sin8 output (0-255) to palette index
             val col = MathUtils.sin8(phase)
             
             // Map to Palette
             val color = getColorFromPalette(col)
             
             // Map i to x,y
             // Assumption: row major or such? Wled used "pixelIndex" which implies linear mapping.
             // BasePixel uses 2D x,y. 
             // Let's assume linear mapping: x + y * width
             val x = i % width
             val y = i / width
             
             if (y < height) setPixelColor(x, y, color)
        }

        return true
    }

    private fun tristateSquare8(time: Long, highValue: Int, lowValue: Int): Int {
        val cycle = (time % 256).toInt()
        return when {
            cycle < 64 -> highValue      
            cycle < 128 -> (highValue + lowValue) / 2  
            cycle < 192 -> lowValue     
            else -> (highValue + lowValue) / 2  
        }
    }
}
