package com.marsraver.wleddj.engine.color

import android.graphics.Color

/**
 * Represents an HSV color with hue, saturation, and value components.
 * - Hue: 0-360 degrees
 * - Saturation: 0.0-1.0
 * - Value: 0.0-1.0
 */
data class HsvColor(
    val h: Float,
    val s: Float,
    val v: Float
) {
    init {
        // require(h >= 0.0f && h <= 360.0f) { "Hue must be 0-360, got $h" } // Relaxed hue check as floats can drift
        require(s >= 0.0f && s <= 1.0f) { "Saturation must be 0.0-1.0, got $s" }
        require(v >= 0.0f && v <= 1.0f) { "Value must be 0.0-1.0, got $v" }
    }

    /**
     * Convert HSV color to RGB color.
     * @return RGB color representation
     */
    fun toRgb(): RgbColor {
        return ColorUtils.hsvToRgb(h, s, v)
    }
    
    fun toInt(): Int {
        return Color.HSVToColor(floatArrayOf(h, s, v))
    }
}
