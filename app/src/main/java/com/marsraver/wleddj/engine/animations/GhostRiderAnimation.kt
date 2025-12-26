package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.random.Random

/**
 * GhostRider - Ghost rider trail effect
 * Migrated to WledDj.
 */
class GhostRiderAnimation : BasePixelAnimation() {

    private data class Lighter(
        var gPosX: Float = 0f,
        var gPosY: Float = 0f,
        var gAngle: Float = 0f,
        var angleSpeed: Float = 0f,
        val lightersPosX: FloatArray = FloatArray(20),
        val lightersPosY: FloatArray = FloatArray(20),
        val angles: FloatArray = FloatArray(20),
        val time: IntArray = IntArray(20),
        val reg: BooleanArray = BooleanArray(20)
    )

    private var lastUpdateNs: Long = 0L
    private lateinit var lighter: Lighter
    private val vSpeed = 5f
    private val maxLighters = 20
    
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        lastUpdateNs = System.nanoTime()

        lighter = Lighter()
        lighter.angleSpeed = (Random.nextInt(20) - 10).toFloat()
        lighter.gAngle = Random.nextFloat() * 360f
        lighter.gPosX = (width / 2f) * 10f
        lighter.gPosY = (height / 2f) * 10f

        for (i in 0 until maxLighters) {
            lighter.lightersPosX[i] = lighter.gPosX
            lighter.lightersPosY[i] = lighter.gPosY + i
            lighter.time[i] = i * 2
            lighter.reg[i] = false
        }
    }

    override fun update(now: Long): Boolean {
         // 1024_000_000L / (width + height)
        val updateInterval = 1024_000_000L / ((width + height).coerceAtLeast(1))
        if (now - lastUpdateNs < updateInterval) return true
        lastUpdateNs = now

        fadeToBlackBy((paramSpeed shr 2) + 64)

        setWuPixel(lighter.gPosX / 10f, lighter.gPosY / 10f, Color.WHITE)

        val angleRad = lighter.gAngle * PI.toFloat() / 180f
        lighter.gPosX += vSpeed * sin(angleRad)
        lighter.gPosY += vSpeed * cos(angleRad)
        lighter.gAngle += lighter.angleSpeed

        if (lighter.gPosX < 0) lighter.gPosX = (width - 1) * 10f
        if (lighter.gPosX > (width - 1) * 10f) lighter.gPosX = 0f
        if (lighter.gPosY < 0) lighter.gPosY = (height - 1) * 10f
        if (lighter.gPosY > (height - 1) * 10f) lighter.gPosY = 0f

        for (i in 0 until maxLighters) {
            lighter.time[i] += Random.nextInt(5, 20)

            if (lighter.time[i] >= 255 ||
                lighter.lightersPosX[i] <= 0 ||
                lighter.lightersPosX[i] >= (width - 1) * 10f ||
                lighter.lightersPosY[i] <= 0 ||
                lighter.lightersPosY[i] >= (height - 1) * 10f) {
                lighter.reg[i] = true
            }

            if (lighter.reg[i]) {
                lighter.lightersPosY[i] = lighter.gPosY
                lighter.lightersPosX[i] = lighter.gPosX
                lighter.angles[i] = lighter.gAngle + (Random.nextInt(20) - 10)
                lighter.time[i] = 0
                lighter.reg[i] = false
            } else {
                val trailAngleRad = lighter.angles[i] * PI.toFloat() / 180f
                lighter.lightersPosX[i] += -7 * sin(trailAngleRad)
                lighter.lightersPosY[i] += -7 * cos(trailAngleRad)
            }

            val paletteIndex = 256 - lighter.time[i]
            val color = getColorFromPalette(paletteIndex)

            // Scale brightness if needed, but here just raw color
            setWuPixel(lighter.lightersPosX[i] / 10f, lighter.lightersPosY[i] / 10f, color)
        }
        
        // blur
        val blurAmount = paramIntensity shr 3
        if (blurAmount > 0) blur2d(blurAmount)
        
        return true
    }

    private fun setWuPixel(x: Float, y: Float, color: Int) {
        // Simple setPixel for now to avoid complexity of full AA implementation in this step
        // Or simplified Wu:
        val xi = x.toInt()
        val yi = y.toInt()
        if (xi in 0 until width && yi in 0 until height) {
            setPixelColor(xi, yi, color)
        }
        // TODO: Full AA implementation?
    }
}
