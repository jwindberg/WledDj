package com.marsraver.wleddj.engine.color

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min

/**
 * Utility functions for color operations.
 */
object ColorUtils {

    /**
     * Convert HSV to RGB color.
     */
    fun hsvToRgb(h: Int, s: Int, v: Int): RgbColor {
        val hue = (h % 256 + 256) % 256
        val saturation = s.coerceIn(0, 255) / 255.0
        val value = v.coerceIn(0, 255) / 255.0

        if (saturation <= 0.0) {
            val gray = (value * 255).toInt()
            return RgbColor(gray, gray, gray)
        }

        val hSection = hue / 42.6666667
        val i = hSection.toInt()
        val f = hSection - i

        val p = value * (1 - saturation)
        val q = value * (1 - saturation * f)
        val t = value * (1 - saturation * (1 - f))

        val (r, g, b) = when (i % 6) {
            0 -> Triple(value, t, p)
            1 -> Triple(q, value, p)
            2 -> Triple(p, value, t)
            3 -> Triple(p, q, value)
            4 -> Triple(t, p, value)
            else -> Triple(value, p, q)
        }

        return RgbColor(
            (r * 255).toInt().coerceIn(0, 255),
            (g * 255).toInt().coerceIn(0, 255),
            (b * 255).toInt().coerceIn(0, 255)
        )
    }

    fun rgbToHsv(color: RgbColor): HsvColor {
        val rNorm = color.r / 255.0f
        val gNorm = color.g / 255.0f
        val bNorm = color.b / 255.0f

        val max = max(rNorm, max(gNorm, bNorm))
        val min = min(rNorm, min(gNorm, bNorm))
        val delta = max - min

        val v = max
        val s = if (max > 0.0f) delta / max else 0.0f

        val h = when {
            delta == 0.0f -> 0.0f
            max == rNorm -> {
                val hRaw = 60.0f * (((gNorm - bNorm) / delta) % 6.0f)
                if (hRaw < 0) hRaw + 360.0f else hRaw
            }
            max == gNorm -> 60.0f * (((bNorm - rNorm) / delta) + 2.0f)
            else -> 60.0f * (((rNorm - gNorm) / delta) + 4.0f)
        }

        return HsvColor(
            h.coerceIn(0.0f, 360.0f),
            s.coerceIn(0.0f, 1.0f),
            v.coerceIn(0.0f, 1.0f)
        )
    }

    fun hsvToRgb(h: Float, s: Float, v: Float): RgbColor {
        if (s <= 0.0f) {
            val gray = (v * 255).toInt()
            return RgbColor(gray, gray, gray)
        }

        val hi = ((h / 60.0f) % 6).toInt()
        val f = h / 60.0f - hi
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)

        val (r, g, b) = when (hi) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }

        return RgbColor(
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }

    fun blend(color1: RgbColor, color2: RgbColor, blendAmount: Int): RgbColor {
        val blend = blendAmount.coerceIn(0, 255) / 255.0
        val invBlend = 1.0 - blend
        return RgbColor(
            ((color1.r * invBlend + color2.r * blend)).toInt().coerceIn(0, 255),
            ((color1.g * invBlend + color2.g * blend)).toInt().coerceIn(0, 255),
            ((color1.b * invBlend + color2.b * blend)).toInt().coerceIn(0, 255)
        )
    }

    fun getAverageLight(color: RgbColor): Int {
        return ((color.r + color.g + color.b) / 3.0).toInt()
    }

    fun scaleBrightness(color: RgbColor, factor: Double): RgbColor {
        val scale = factor.coerceIn(0.0, 1.0)
        return RgbColor(
            (color.r * scale).toInt().coerceIn(0, 255),
            (color.g * scale).toInt().coerceIn(0, 255),
            (color.b * scale).toInt().coerceIn(0, 255)
        )
    }

    fun sin8(angle: Int): Int {
        val normalized = (angle % 256 + 256) % 256
        val radians = (normalized / 255.0) * 2 * PI
        val sine = sin(radians)
        val result = ((sine + 1.0) / 2.0 * 255.0).toInt()
        return result.coerceIn(0, 255)
    }
    
    fun heatColor(temperature: Int): RgbColor {
        val t = temperature.coerceIn(0, 255)
        val t192 = (t * 191) / 255
        var heatramp = t192 and 0x3F
        heatramp = heatramp shl 2

        return if (t192 and 0x80 != 0) {
            RgbColor(255, 255, heatramp)
        } else if (t192 and 0x40 != 0) {
            RgbColor(255, heatramp, 0)
        } else {
            RgbColor(heatramp, 0, 0)
        }
    }
    
    fun fade(color: RgbColor, amount: Int): RgbColor {
        return RgbColor(
            (color.r - amount).coerceAtLeast(0),
            (color.g - amount).coerceAtLeast(0),
            (color.b - amount).coerceAtLeast(0)
        )
    }
}
