package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.math.NoiseUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Plasma RotoZoomer animation
 * Migrated to WledDj.
 */
class RotoZoomerAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private lateinit var plasma: Array<IntArray>
    private var alt: Boolean = false // Could be a config
    
    private var angle: Float = 0.0f
    private var startTimeNs: Long = 0L

    override fun onInit() {
        plasma = Array(height) { IntArray(width) }
        startTimeNs = System.nanoTime()
        angle = 0.0f
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000
        val ms = timeMs / 120 // Even slower plasma evolution (was 40, originally 15)
        
        // Generate plasma pattern
        for (j in 0 until height) {
            for (i in 0 until width) {
                if (alt) {
                    plasma[j][i] = (((i * 4) xor (j * 4)) + (ms / 6).toInt()) and 0xFF
                } else {
                    plasma[j][i] = MathUtils.inoise8(i * 40, j * 40, ms.toInt())
                }
            }
        }
        
        val sinHalf = sin(angle / 2.0).toFloat()
        val intensityFactor = (128 - paramIntensity) / 128.0f
        val f = (sinHalf + intensityFactor + 1.1f) / 1.5f
        
        val kosinus = cos(angle.toDouble()).toFloat() * f
        val sinus = sin(angle.toDouble()).toFloat() * f
        
        for (i in 0 until width) {
            val u1 = i * kosinus
            val v1 = i * sinus
            for (j in 0 until height) {
                val u = (abs((u1 - j * sinus).toInt()) and 0xFF) % width
                val v = (abs((v1 + j * kosinus).toInt()) and 0xFF) % height
                val plasmaValue = plasma[v][u]
                
                // Color mapping: val = Hue? (now Palette Index)
                val color = getColorFromPalette(plasmaValue)
                setPixelColor(i, j, color)
            }
        }
        
        val speedFactor = (paramSpeed - 128) * 0.0002f
        angle -= 0.01f + speedFactor // Slower rotation (was 0.03f)
        
        val maxAngle = 1000.0f * 2.0f * PI.toFloat()
        if (angle < -maxAngle) angle += maxAngle
        if (angle > maxAngle) angle -= maxAngle
        
        return true
    }
}
