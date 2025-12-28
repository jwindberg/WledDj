package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Julia animation - Animated Julia set fractal
 * Migrated to WledDj.
 */
class JuliaAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private data class JuliaState(
        var xcen: Float = 0.0f,
        var ycen: Float = 0.0f,
        var xymag: Float = 1.0f
    )
    
    // Custom defaults
    private var custom1: Int = 128
    private var custom2: Int = 128
    private var custom3: Int = 16
    
    private var juliaState = JuliaState()
    private var startTimeNs: Long = 0L
    private var initialized: Boolean = false

    override fun onInit() {
        startTimeNs = System.nanoTime()
        paramIntensity = 24
        
        if (!initialized) {
            juliaState.xcen = 0.0f
            juliaState.ycen = 0.0f
            juliaState.xymag = 1.0f
            initialized = true
        }
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = System.nanoTime()
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000

        // Update Julia state
        juliaState.xcen += (custom1 - 128) / 100000.0f
        juliaState.ycen += (custom2 - 128) / 100000.0f
        juliaState.xymag += ((custom3 - 16) shl 3) / 100000.0f

        // Constrain
        if (juliaState.xymag < 0.01f) juliaState.xymag = 0.01f
        if (juliaState.xymag > 1.0f) juliaState.xymag = 1.0f

        // Bounds
        var xmin = juliaState.xcen - juliaState.xymag
        var xmax = juliaState.xcen + juliaState.xymag
        var ymin = juliaState.ycen - juliaState.xymag
        var ymax = juliaState.ycen + juliaState.xymag

        // Constrain bounds
        xmin = xmin.coerceIn(-1.2f, 1.2f)
        xmax = xmax.coerceIn(-1.2f, 1.2f)
        ymin = ymin.coerceIn(-0.8f, 1.0f)
        ymax = ymax.coerceIn(-0.8f, 1.0f)

        val dx = (xmax - xmin) / width
        val dy = (ymax - ymin) / height

        val maxIterations = (paramIntensity / 2).coerceAtLeast(1)
        val maxCalc = 16.0f

        var reAl = -0.94299f 
        var imAg = 0.3162f

        reAl += sin16_t(timeMs * 34) / 655340.0f
        imAg += sin16_t(timeMs * 26) / 655340.0f

        var y = ymin
        for (j in 0 until height) {
            var x = xmin
            for (i in 0 until width) {
                var a = x
                var b = y
                var iter = 0

                while (iter < maxIterations) {
                    val aa = a * a
                    val bb = b * b
                    val len = aa + bb

                    if (len > maxCalc) break

                    b = 2 * a * b + imAg
                    a = aa - bb + reAl
                    iter++
                }

                if (iter == maxIterations) {
                    setPixelColor(i, j, Color.BLACK)
                } else {
                    val colorIndex = (iter * 255 / maxIterations).coerceIn(0, 255)
                    val color = getColorFromPalette(colorIndex)
                    setPixelColor(i, j, color)
                }
                x += dx
            }
            y += dy
        }
        return true
    }

    private fun sin16_t(input: Long): Int {
        val normalized = (input % 65536) / 65536.0
        val radians = normalized * 2.0 * PI
        val sine = sin(radians)
        return (sine * 32767.0).roundToInt()
    }
}
