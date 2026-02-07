package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.audio.FftMeter
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Akemi animation - renders a stylised character with audio-reactive elements and side GEQ bars.
 * Migrated to WledDj.
 */
class AkemiAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var fftMeter: FftMeter? = null
    private val numBands: Int = 16

    override fun onInit() {
        fftMeter = FftMeter(bands = numBands)
        // Keep resolution consistent with the design logic of Akemi 
        // (It expects scalable, but BaseWidth is 32x32 logic?)
        // Let's rely on the base 64x64 or similar.
    }

    override fun update(now: Long): Boolean {
        // Use normalized bands for better sensitivity across different volumes
        val spectrumSnapshot = fftMeter?.getNormalizedBands() ?: IntArray(numBands)
        val timeMs = now / 1L // Already ms in WledDj (BasePixel passes System.millis)
        // Note: WledFx update(now) passes nanoseconds usually? No, WledFx is usually millis-ish or ticks.
        // Let's assume standard millis.

        val speedFactor = (paramSpeed shr 2) + 2
        var counter = ((timeMs * speedFactor) and 0xFFFF).toInt() // adjusted time scale
        counter = counter shr 8

        val lightFactor = 0.15f
        val normalFactor = 0.4f

        val soundColor = Color.rgb(255, 165, 0)
        val armsAndLegsDefault = Color.rgb(0xFF, 0xE0, 0xA0)
        val eyeColor = Color.WHITE

        val faceColor = colorWheel(counter and 0xFF)
        val armsAndLegsColor = armsAndLegsDefault

        val base = spectrumSnapshot.getOrElse(0) { 0 } / 255.0f
        // More sensitive dancing trigger: responds at lower intensities and lower audio levels
        val isDancing = paramIntensity > 64 && spectrumSnapshot.getOrElse(0) { 0 } > 64

        if (isDancing) {
            for (x in 0 until width) {
                setPixelColor(x, 0, Color.BLACK)
            }
        }

        for (y in 0 until height) {
            val akY = min(BASE_HEIGHT - 1, y * BASE_HEIGHT / height)
            for (x in 0 until width) {
                val akX = min(BASE_WIDTH - 1, x * BASE_WIDTH / width)
                val MapIdx = akY * BASE_WIDTH + akX
                val ak = if (MapIdx < AKEMI_MAP.size) AKEMI_MAP[MapIdx] else 0

                val color = when (ak) {
                    3 -> multiplyColor(armsAndLegsColor, lightFactor)
                    2 -> multiplyColor(armsAndLegsColor, normalFactor)
                    1 -> armsAndLegsColor
                    6 -> multiplyColor(faceColor, lightFactor)
                    5 -> multiplyColor(faceColor, normalFactor)
                    4 -> faceColor
                    7 -> eyeColor
                    8 -> if (base > 0.2f) {
                        // Boost sound color more aggressively but still clamp to valid range
                        val boost = clamp01(base * 1.8f)
                        Color.rgb(
                            min(255, (Color.red(soundColor) * boost).roundToInt()),
                            min(255, (Color.green(soundColor) * boost).roundToInt()),
                            min(255, (Color.blue(soundColor) * boost).roundToInt())
                        )
                    } else {
                        armsAndLegsColor
                    }
                    else -> Color.BLACK
                }

                if (isDancing) {
                    val targetY = min(height - 1, y + 1)
                    setPixelColor(x, targetY, color)
                } else {
                    setPixelColor(x, y, color)
                }
            }
        }

        // Draw Bars
        val xMax = max(1, width / 8)
        val midY = height / 2
        val maxBarHeight = max(1, 17 * height / 32)

        for (x in 0 until xMax) {
            var band = mapValue(x, 0, max(xMax, 4), 0, 15)
            band = constrain(band, 0, 15)
            var barHeight = mapValue(spectrumSnapshot.getOrElse(band) { 0 }, 0, 255, 0, maxBarHeight)
            barHeight = barHeight.coerceIn(0, maxBarHeight)

            val colorIndex = band * 16 // Adjusted spacing for palette
            val barColor = getColorFromPalette(colorIndex % 256)

            for (y in 0 until barHeight) {
                val topY = midY - y
                if (topY in 0 until height) {
                    setPixelColor(x, topY, barColor)
                    val mirrorX = width - 1 - x
                    setPixelColor(mirrorX, topY, barColor)
                }
            }
        }
        return true
    }

    override fun destroy() {
        fftMeter?.stop()
        fftMeter = null
    }

    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    private fun constrain(value: Int, minVal: Int, maxVal: Int): Int = value.coerceIn(minVal, maxVal)
    private fun clamp01(value: Float): Float = value.coerceIn(0f, 1f)

    private fun multiplyColor(color: Int, factor: Float): Int {
        val clampedFactor = clamp01(factor)
        return Color.rgb(
            min(255, (Color.red(color) * clampedFactor).roundToInt()),
            min(255, (Color.green(color) * clampedFactor).roundToInt()),
            min(255, (Color.blue(color) * clampedFactor).roundToInt())
        )
    }

    companion object {
        private const val BASE_WIDTH = 32
        private const val BASE_HEIGHT = 32
        private val AKEMI_MAP = intArrayOf(
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,2,2,3,3,3,3,3,3,2,2,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,2,3,3,0,0,0,0,0,0,3,3,2,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,2,3,0,0,0,6,5,5,4,0,0,0,3,2,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,2,3,0,0,6,6,5,5,5,5,4,4,0,0,3,2,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,2,3,0,6,5,5,5,5,5,5,5,5,4,0,3,2,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,2,3,0,6,5,5,5,5,5,5,5,5,5,5,4,0,3,2,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,3,2,0,6,5,5,5,5,5,5,5,5,5,5,4,0,2,3,0,0,0,0,0,0,0,
            0,0,0,0,0,0,3,2,3,6,5,5,7,7,5,5,5,5,7,7,5,5,4,3,2,3,0,0,0,0,0,0,
            0,0,0,0,0,2,3,1,3,6,5,1,7,7,7,5,5,1,7,7,7,5,4,3,1,3,2,0,0,0,0,0,
            0,0,0,0,0,8,3,1,3,6,5,1,7,7,7,5,5,1,7,7,7,5,4,3,1,3,8,0,0,0,0,0,
            0,0,0,0,0,8,3,1,3,6,5,5,1,1,5,5,5,5,1,1,5,5,4,3,1,3,8,0,0,0,0,0,
            0,0,0,0,0,2,3,1,3,6,5,5,5,5,5,5,5,5,5,5,5,5,4,3,1,3,2,0,0,0,0,0,
            0,0,0,0,0,0,3,2,3,6,5,5,5,5,5,5,5,5,5,5,5,5,4,3,2,3,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,6,5,5,5,5,5,7,7,5,5,5,5,5,4,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,0,0,0,0,
            1,0,0,0,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,0,0,0,2,
            0,2,2,2,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,2,2,2,0,
            0,0,0,3,2,0,0,0,6,5,4,4,4,4,4,4,4,4,4,4,4,4,4,4,0,0,0,2,2,0,0,0,
            0,0,0,3,2,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,2,3,0,0,0,
            0,0,0,0,3,2,0,0,0,0,3,3,0,3,3,0,0,3,3,0,3,3,0,0,0,0,2,2,0,0,0,0,
            0,0,0,0,3,2,0,0,0,0,3,2,0,3,2,0,0,3,2,0,3,2,0,0,0,0,2,3,0,0,0,0,
            0,0,0,0,0,3,2,0,0,3,2,0,0,3,2,0,0,3,2,0,0,3,2,0,0,2,3,0,0,0,0,0,
            0,0,0,0,0,3,2,2,2,2,0,0,0,3,2,0,0,3,2,0,0,0,3,2,2,2,3,0,0,0,0,0,
            0,0,0,0,0,0,3,3,3,0,0,0,0,3,2,0,0,3,2,0,0,0,0,3,3,3,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
        )
    }
}
