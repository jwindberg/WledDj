package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Hiphotic animation - Nested sine/cosine pattern
 * Migrated to WledDj.
 */
class HiphoticAnimation : BasePixelAnimation() {
    
    override fun supportsPalette(): Boolean = true

    private var custom3: Int = 128 
    private var startTimeNs: Long = 0L

    override fun onInit() {
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000
        
        val a = (timeMs / ((custom3 shr 1) + 1).coerceAtLeast(1)).toInt()
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val cosArg = (x * paramSpeed / 16 + a / 3) and 0xFF
                val cosVal = MathUtils.cos8(cosArg)
                
                val sinArg = (y * paramIntensity / 16 + a / 4) and 0xFF
                val sinVal = MathUtils.sin8(sinArg)
                
                val finalArg = (cosVal + sinVal + a) and 0xFF
                val colorIndex = MathUtils.sin8(finalArg)
                
                val color = getColorFromPalette(colorIndex)
                setPixelColor(x, y, color)
            }
        }
        return true
    }
}
