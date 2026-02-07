package com.marsraver.wleddj.engine.color

import android.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class Palette(val displayName: String, val colors: Array<RgbColor>) {
    
    @SerialName("Standard") STANDARD("Standard", arrayOf(
        RgbColor(255, 0, 0),     // Red
        RgbColor(0, 255, 0),     // Green
        RgbColor(0, 0, 255),     // Blue
        RgbColor(255, 255, 0),   // Yellow
        RgbColor(0, 255, 255),   // Cyan
        RgbColor(255, 0, 255),   // Magenta
        RgbColor(255, 255, 255), // White
        RgbColor(0, 0, 0),       // Black
        RgbColor(128, 128, 128), // Gray
        RgbColor(255, 152, 0),   // Orange
        RgbColor(156, 39, 176),  // Purple
        RgbColor(0, 150, 136),   // Teal
        RgbColor(255, 193, 7),   // Amber
        RgbColor(63, 81, 181),   // Indigo
        RgbColor(233, 30, 99),   // Pink
        RgbColor(205, 220, 57)   // Lime
    )),

    @SerialName("Rainbow") RAINBOW("Rainbow", arrayOf(
        RgbColor(255, 0, 0),   // Red
        RgbColor(255, 127, 0), // Orange
        RgbColor(255, 255, 0), // Yellow
        RgbColor(127, 255, 0), // Yellow-Green
        RgbColor(0, 255, 0),   // Green
        RgbColor(0, 255, 127), // Green-Cyan
        RgbColor(0, 255, 255), // Cyan
        RgbColor(0, 127, 255), // Cyan-Blue
        RgbColor(0, 0, 255),   // Blue
        RgbColor(127, 0, 255), // Blue-Purple
        RgbColor(255, 0, 255), // Magenta
        RgbColor(255, 0, 127)  // Magenta-Red
    )),

    @SerialName("Party") PARTY("Party", arrayOf(
        RgbColor(255, 0, 0),   // Red
        RgbColor(255, 0, 255), // Magenta
        RgbColor(0, 0, 255),   // Blue
        RgbColor(0, 255, 255), // Cyan
        RgbColor(0, 255, 0),   // Green
        RgbColor(255, 255, 0), // Yellow
        RgbColor(255, 127, 0), // Orange
        RgbColor(255, 0, 0)    // Red
    )),

    @SerialName("Ocean") OCEAN("Ocean", arrayOf(
        RgbColor(0, 0, 128),   // Dark Blue
        RgbColor(0, 0, 255),   // Blue
        RgbColor(0, 127, 255), // Light Blue
        RgbColor(0, 255, 255), // Cyan
        RgbColor(64, 224, 208), // Turquoise
        RgbColor(0, 255, 255), // Cyan
        RgbColor(0, 127, 255), // Light Blue
        RgbColor(0, 0, 255)    // Blue
    )),

    @SerialName("Forest") FOREST("Forest", arrayOf(
        RgbColor(0, 64, 0),    // Dark Green
        RgbColor(0, 128, 0),  // Green
        RgbColor(0, 255, 0),   // Bright Green
        RgbColor(127, 255, 0), // Yellow-Green
        RgbColor(255, 255, 0), // Yellow
        RgbColor(127, 255, 0), // Yellow-Green
        RgbColor(0, 255, 0),   // Bright Green
        RgbColor(0, 128, 0)   // Green
    )),

    @SerialName("Lava") LAVA("Lava", arrayOf(
        RgbColor(0, 0, 0),    // Black
        RgbColor(64, 0, 0),   // Dark Red
        RgbColor(128, 0, 0),  // Red
        RgbColor(255, 0, 0),  // Bright Red
        RgbColor(255, 64, 0), // Orange-Red
        RgbColor(255, 127, 0), // Orange
        RgbColor(255, 64, 0), // Orange-Red
        RgbColor(255, 0, 0)   // Bright Red
    )),

    @SerialName("Cloud") CLOUD("Cloud", arrayOf(
        RgbColor(64, 64, 64),  // Dark Gray
        RgbColor(128, 128, 128), // Gray
        RgbColor(192, 192, 192), // Light Gray
        RgbColor(255, 255, 255), // White
        RgbColor(192, 192, 192), // Light Gray
        RgbColor(128, 128, 128), // Gray
        RgbColor(64, 64, 64),  // Dark Gray
        RgbColor(32, 32, 32)   // Very Dark Gray
    )),

    @SerialName("Sunset") SUNSET("Sunset", arrayOf(
        RgbColor(0, 0, 0),    // Black
        RgbColor(64, 0, 64),  // Dark Purple
        RgbColor(128, 0, 128), // Purple
        RgbColor(255, 0, 255), // Magenta
        RgbColor(255, 64, 0), // Orange-Red
        RgbColor(255, 127, 0), // Orange
        RgbColor(255, 191, 0), // Yellow-Orange
        RgbColor(255, 255, 0)  // Yellow
    )),

    @SerialName("Heat") HEAT("Heat", arrayOf(
        RgbColor(0, 0, 0),    // Black
        RgbColor(64, 0, 0),   // Dark Red
        RgbColor(128, 0, 0),  // Red
        RgbColor(255, 0, 0),  // Bright Red
        RgbColor(255, 64, 0), // Orange-Red
        RgbColor(255, 127, 0), // Orange
        RgbColor(255, 191, 0), // Yellow-Orange
        RgbColor(255, 255, 255) // White
    )),

    @SerialName("Ice") ICE("Ice", arrayOf(
        RgbColor(0, 0, 0),    // Black
        RgbColor(0, 0, 64),   // Dark Blue
        RgbColor(0, 0, 128),  // Blue
        RgbColor(0, 0, 255),  // Bright Blue
        RgbColor(0, 64, 255), // Light Blue
        RgbColor(0, 127, 255), // Cyan-Blue
        RgbColor(0, 255, 255), // Cyan
        RgbColor(255, 255, 255) // White
    )),

    @SerialName("Christmas") CHRISTMAS("Christmas", arrayOf(
        RgbColor(255, 0, 0),    // Red
        RgbColor(0, 255, 0),    // Green
        RgbColor(255, 215, 0),  // Gold
        RgbColor(255, 0, 0),    // Red
        RgbColor(0, 255, 0),    // Green
        RgbColor(192, 192, 192),// Silver
        RgbColor(255, 0, 0),    // Red
        RgbColor(0, 255, 0)     // Green
    )),

    @SerialName("Cytoplasmic") CYTOPLASMIC("Cytoplasmic Gold", arrayOf(
        RgbColor(85, 107, 47),   // Dark Olive Green (Deep Base)
        RgbColor(128, 128, 0),   // Olive (Mid Base)
        RgbColor(218, 165, 32),  // Goldenrod
        RgbColor(255, 215, 0),   // Gold
        RgbColor(255, 255, 0),   // Yellow
        RgbColor(173, 255, 47),  // Green Yellow (Highlight)
        RgbColor(127, 255, 0),   // Chartreuse (Touch of Green)
        RgbColor(255, 255, 224)  // Light Yellow (Peak Brightness)
    ));

    /**
     * Get color at normalized position (0.0-1.0).
     */
    fun getColorAt(position: Double): RgbColor {
        if (colors.isEmpty()) return RgbColor.BLACK
        val pos = position.coerceIn(0.0, 1.0)
        val index = (pos * colors.size).toInt().coerceIn(0, colors.size - 1)
        return colors[index]
    }

    /**
     * Get interpolated color for index 0-255.
     * Maps 0-255 to the full cyclic palette range with linear blending.
     */
    fun getInterpolatedInt(index: Int): Int {
        if (colors.isEmpty()) return Color.BLACK
        if (colors.size == 1) return colors[0].toInt()

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

}
