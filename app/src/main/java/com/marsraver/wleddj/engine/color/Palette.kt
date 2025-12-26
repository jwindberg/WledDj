package com.marsraver.wleddj.engine.color

import android.graphics.Color

/**
 * Represents a color palette with a name and array of RGB colors.
 */
data class Palette(
    val name: String,
    val colors: Array<RgbColor>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Palette

        if (name != other.name) return false
        if (!colors.contentDeepEquals(other.colors)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + colors.contentDeepHashCode()
        return result
    }

    /**
     * Get color at index, wrapping if needed.
     */
    fun getColor(index: Int): RgbColor {
        if (colors.isEmpty()) return RgbColor.BLACK
        val idx = (index % colors.size + colors.size) % colors.size
        return colors[idx]
    }
    
    /**
     * Get Int color (ARGB).
     */
    fun getColorInt(index: Int): Int {
        return getColor(index).toInt()
    }

    /**
     * Get color at normalized position (0.0-1.0).
     */
    fun getColorAt(position: Double): RgbColor {
        if (colors.isEmpty()) return RgbColor.BLACK
        val pos = position.coerceIn(0.0, 1.0)
        val index = (pos * colors.size).toInt().coerceIn(0, colors.size - 1)
        return colors[index]
    }
    
    fun getColorIntAt(position: Double): Int {
        return getColorAt(position).toInt()
    }

    /**
     * Get interpolated color for index 0-255.
     * Maps 0-255 to the full cyclic palette range with linear blending.
     */
    fun getInterpolatedInt(index: Int): Int {
        if (colors.isEmpty()) return Color.BLACK
        if (colors.size == 1) return colors[0].toInt()

        // Normalize 0-255 to 0.0 - colors.size
        // FastLED usually treats 255 as wrapping back to 0
        // pos = (index % 256) / 256.0 * size
        val i = index and 0xFF // clamp to 0-255
        val pos = (i / 256.0) * colors.size
        
        val idx1 = pos.toInt() % colors.size
        val idx2 = (idx1 + 1) % colors.size
        val fraction = pos - idx1

        val c1 = colors[idx1]
        val c2 = colors[idx2]

        val r = (c1.r + (c2.r - c1.r) * fraction).toInt()
        val g = (c1.g + (c2.g - c1.g) * fraction).toInt()
        val b = (c1.b + (c2.b - c1.b) * fraction).toInt()

        return Color.rgb(r, g, b)
    }

    companion object {
        /**
         * Returns a randomly selected palette from all available palettes.
         * @return a randomly selected Palette
         */
        fun getRandom(): Palette {
            val allPalettes = Palettes.all.values.toList()
            return if (allPalettes.isNotEmpty()) {
                allPalettes.random()
            } else {
                Palettes.getDefault()
            }
        }
    }
}
