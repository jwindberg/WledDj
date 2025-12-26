package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

/**
 * Puddles animation - Audio-reactive puddles of light
 * Migrated to WledDj.
 */
class PuddlesAnimation : BasePixelAnimation() {

    override fun supportsPrimaryColor(): Boolean = false
    override fun supportsPalette(): Boolean = true

    private var peakDetect: Boolean = false
    private var custom1: Int = 0
    private var custom2: Int = 0 

    private var loudnessMeter: LoudnessMeter? = null
    private var fftMeter: FftMeter? = null
    private var startTimeNs: Long = 0L
    private val random = Random.Default
    private var lastPuddleTime: Long = 0L

    override fun onInit() {
        startTimeNs = System.nanoTime()
        lastPuddleTime = 0L
        loudnessMeter = LoudnessMeter()
        fftMeter = FftMeter(bands = 32)
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000

        // Speed controls fade rate
        val fadeVal = MathUtils.map(paramSpeed, 0, 255, 15, 1) // WledFx uses subtractive map(240, 254) -> so 15 to 1 fade?
        // WledFx: fadeOut(map(240, 254)) -> means it subtracts a small amount.
        // fadeToBlackBy takes amount to subtract. 
        // 255-240 = 15. 255-254=1.
        
        fadeToBlackBy(fadeVal)

        val loudness = loudnessMeter?.getNormalizedLoudness() ?: 0
        val rawVol = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        val smoothVol = loudness / 1024.0f * 255.0f

        val fftBands = fftMeter?.getNormalizedBands() ?: IntArray(32)
        val maxValue = fftBands.maxOrNull() ?: 0
        val totalMagnitude = fftBands.sum()

        val threshold = 50
        val peak = maxValue > threshold
        val vol = (totalMagnitude / 16.0).toInt().coerceIn(0, 255)

        val minInterval = MathUtils.map(paramSpeed, 0, 255, 50, 10).toLong()
        val timeSinceLastPuddle = timeMs - lastPuddleTime
        val canCreatePuddle = timeSinceLastPuddle >= minInterval

        var size = 0
        var posX = 0
        var posY = 0
        var shouldCreatePuddle = false

        if (peakDetect) {
            val volumeThreshold = custom2 / 2
            if (canCreatePuddle && peak && vol >= volumeThreshold) {
                shouldCreatePuddle = true
                lastPuddleTime = timeMs
                posX = random.nextInt(width)
                posY = random.nextInt(height)
                size = (smoothVol * paramIntensity / 256.0f / 4.0f + 1.0f).toInt().coerceAtLeast(1)
            }
        } else {
            // Raw volume check > 1. Normalized loudness should trigger this easily.
            if (canCreatePuddle && rawVol > 1) {
                shouldCreatePuddle = true
                lastPuddleTime = timeMs
                posX = random.nextInt(width)
                posY = random.nextInt(height)
                size = (rawVol * paramIntensity / 256 / 8 + 1).coerceAtLeast(1)
            }
        }

        if (shouldCreatePuddle && size > 0) {
            val maxRadius = min(width, height) / 2
            size = min(size, maxRadius)

            if (size > 0) {
                val colorIndex = (timeMs % 256).toInt()
                val color = getColorFromPalette(colorIndex)
                drawPuddle(posX, posY, size, color)
            }
        }

        return true
    }

    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
        fftMeter?.stop()
        fftMeter = null
    }

    private fun drawPuddle(centerX: Int, centerY: Int, radius: Int, rgb: Int) {
        if (radius <= 0) {
            setPixelColor(centerX, centerY, rgb)
            return
        }
        val minX = max(0, centerX - radius)
        val maxX = min(width - 1, centerX + radius)
        val minY = max(0, centerY - radius)
        val maxY = min(height - 1, centerY + radius)
        val radiusSq = radius * radius

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val dx = x - centerX
                val dy = y - centerY
                if (dx*dx + dy*dy <= radiusSq) {
                    setPixelColor(x, y, rgb)
                }
            }
        }
    }
}
