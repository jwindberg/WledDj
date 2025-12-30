package com.marsraver.wleddj.animations

import android.graphics.Color
import kotlin.random.Random

/**
 * Fire 2012 2D
 * Vertical Fire simulation for 2D Matrices.
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palette

class Fire2012_2DAnimation : BasePixelAnimation() {
    
    override fun getDefaultPalette(): Palette = Palette.HEAT

    private lateinit var heat: IntArray // width * height
    
    override fun onInit() {
        heat = IntArray(width * height)
    }
    
    override fun update(now: Long): Boolean {
        if (width == 0 || height == 0) return true
        
        // Cooling / Sparking
        val cooling = (paramSpeed * 0.5).toInt().coerceAtLeast(20) 
        val sparking = paramIntensity.coerceIn(0, 255)
        
        for (x in 0 until width) {
            val offset = x * height
            
            // 1. Cool down
            for (y in 0 until height) {
                val cooldown = Random.nextInt(0, ((cooling * 10) / height) + 2)
                heat[offset + y] = (heat[offset + y] - cooldown).coerceAtLeast(0)
            }
            
            // 2. Drift Up. logic: heat drifts up with smoothing
            for (k in height - 1 downTo 2) {
                heat[offset + k] = (heat[offset + k - 1] + heat[offset + k - 2] + heat[offset + k - 2]) / 3
            }
            
            // 3. Spark
            if (Random.nextInt(0, 255) < sparking) {
                val y = Random.nextInt(0, 3.coerceAtMost(height))
                heat[offset + y] = (heat[offset + y] + Random.nextInt(160, 255)).coerceAtMost(255)
            }
        
            // 4. Map heat to color
            for (y in 0 until height) {
                 // Map y (0=Top) to fire height (0=Bottom Source)
                 val rowIndex = (height - 1 - y).coerceIn(0, height - 1)
                 val heatIndex = offset + rowIndex
                 val t = heat[heatIndex]
                 // Simple heat color mapping if no palette
                 // Simple heat color mapping if no palette
                 val color = getColorFromPalette(t)
                 setPixelColor(x, y, color)
            }
        }
        return true
    }
    

}
