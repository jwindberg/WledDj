package com.marsraver.wleddj.engine.math

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.roundToInt

object MathUtils {

    /**
     * sin16 - 16-bit sine function
     * Returns value from -32768 to 32767
     */
    fun sin16(angle: Int): Int {
        // Convert angle to 0-65535 range
        val normalizedAngle = (angle % 65536)
        // Convert to radians (0-65535 maps to 0-2π)
        val radians = normalizedAngle * 2.0 * PI / 65536.0
        val sine = sin(radians)
        // Map from -1..1 to -32768..32767
        return (sine * 32767.0).toInt().coerceIn(-32768, 32767)
    }

    /**
     * triwave8 - Triangular wave function (0-255 input, 0-255 output)
     */
    fun triwave8(input: Int): Int {
        val inVal = input and 0xFF
        return if (inVal < 128) {
            inVal * 2
        } else {
            255 - (inVal - 128) * 2
        }
    }

    /**
     * Scale a value by another (8-bit)
     * (value * scale) / 256
     */
    fun scale8(value: Int, scale: Int): Int {
        return ((value and 0xFF) * (scale and 0xFF)) / 256
    }

    /**
     * sin8 - Sine function for 8-bit input
     * Maps 0-255 input to 0-255 output (shifted sine wave)
     */
    fun sin8(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val radians = normalized * 2.0 * PI
        val sine = sin(radians)
        return ((sine + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
    }

    /**
     * cos8 - Cosine function for 8-bit input (0-255 maps to 0-2π)
     */
    fun cos8(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val radians = normalized * 2.0 * PI
        val cosine = cos(radians)
        return ((cosine + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
    }

    /**
     * cubicwave8 - Cubic wave function
     */
    fun cubicwave8(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val phase = normalized * 2.0 * PI
        val sine = sin(phase)
        val cubic = 3.0 * sine - sine * sine * sine
        return ((cubic + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
    }

    /**
     * beat8 - Generates an 8-bit sawtooth wave at a given BPM
     */
    fun beat8(bpm: Int, timeMs: Long): Int {
        return ((timeMs * bpm / 60000.0 * 256.0) % 256).toInt()
    }

    /**
     * beatsin8 - Generates an 8-bit sine wave at a given BPM
     */
    fun beatsin8(bpm: Int, low: Int, high: Int, timeMs: Long, phaseOffset: Int = 0): Int {
        val beat = ((timeMs * bpm / 60000.0) + (phaseOffset / 256.0)) % 1.0
        val radians = beat * 2 * PI
        val sine = sin(radians)
        val normalized = (sine + 1.0) / 2.0
        return (low + (normalized * (high - low))).toInt().coerceIn(low, high)
    }

    /**
     * beatsin16 - Generates a 16-bit sine wave at a given BPM
     */
    fun beatsin16(bpm: Int, low: Int, high: Int, timeMs: Long, phaseOffset: Int = 0): Int {
        val beat = ((timeMs * bpm / 60000.0) + (phaseOffset / 65536.0)) % 1.0
        val radians = beat * 2 * PI
        val sine = sin(radians)
        val normalized = (sine + 1.0) / 2.0
        return (low + (normalized * (high - low))).toInt().coerceIn(low, high)
    }

    /**
     * qadd8 - Saturating addition for 8-bit values
     */
    fun qadd8(a: Int, b: Int): Int {
        return (a + b).coerceAtMost(255)
    }

    /**
     * qsub8 - Saturating subtraction for 8-bit values
     */
    fun qsub8(a: Int, b: Int): Int {
        return (a - b).coerceAtLeast(0)
    }

    /**
     * Map a value from one range to another (Int)
     */
    fun map(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        val fromRange = (inMax - inMin).toDouble()
        val toRange = (outMax - outMin).toDouble()
        if (fromRange == 0.0) return outMin
        val scaled = (value - inMin) / fromRange
        return (outMin + scaled * toRange).roundToInt()
    }

    /**
     * Map a value from one range to another (Double)
     */
    fun map(value: Int, inMin: Int, inMax: Int, outMin: Double, outMax: Double): Double {
        val fromRange = (inMax - inMin).toDouble()
        val toRange = (outMax - outMin)
        if (fromRange == 0.0) return outMin
        val scaled = (value - inMin) / fromRange
        return (outMin + scaled * toRange)
    }

    /**
     * Map a value from one range to another (Float)
     */
    fun mapf(value: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float {
        val fromRange = fromHigh - fromLow
        val toRange = toHigh - toLow
        if (fromRange == 0.0f) return toLow
        val scaled = (value - fromLow) / fromRange
        return toLow + scaled * toRange
    }

    /**
     * inoise8 - 8-bit Perlin noise function
     */
    fun inoise8(x: Int, y: Int): Int {
        val xi = x
        val yi = y
        // Simple hash for noise
        val hash = ((xi * 2654435761L + yi * 2246822519L) and 0xFFFFFFFF).toInt()
        return (hash and 0xFF)
    }

    fun inoise8(x: Float, y: Float): Int {
        return inoise8(x.toInt(), y.toInt())
    }

    /**
     * inoise8 - 8-bit Perlin noise function 3D
     */
    fun inoise8(x: Int, y: Int, z: Int): Int {
        val xi = x
        val yi = y
        val zi = z
        // Simple hash for noise
        val hash = ((xi * 2654435761L + yi * 2246822519L + zi * 3266489917L) and 0xFFFFFFFF).toInt()
        return (hash and 0xFF)
    }

    /**
     * Perlin noise implementation (Improved Perlin Noise)
     * Returns value roughly between -1.0 and 1.0
     */
    fun perlinNoise(x: Double, y: Double, z: Double): Double {
        val xi = kotlin.math.floor(x).toInt() and PERM_MASK
        val yi = kotlin.math.floor(y).toInt() and PERM_MASK
        val zi = kotlin.math.floor(z).toInt() and PERM_MASK

        val xf = x - kotlin.math.floor(x)
        val yf = y - kotlin.math.floor(y)
        val zf = z - kotlin.math.floor(z)

        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        val aaa = PERM[PERM[PERM[xi] + yi] + zi]
        val aba = PERM[PERM[PERM[xi] + ((yi + 1) and PERM_MASK)] + zi]
        val aab = PERM[PERM[PERM[xi] + yi] + ((zi + 1) and PERM_MASK)]
        val abb = PERM[PERM[PERM[xi] + ((yi + 1) and PERM_MASK)] + ((zi + 1) and PERM_MASK)]
        val baa = PERM[PERM[PERM[(xi + 1) and PERM_MASK] + yi] + zi]
        val bba = PERM[PERM[PERM[(xi + 1) and PERM_MASK] + ((yi + 1) and PERM_MASK)] + zi]
        val bab = PERM[PERM[PERM[(xi + 1) and PERM_MASK] + yi] + ((zi + 1) and PERM_MASK)]
        val bbb = PERM[PERM[PERM[(xi + 1) and PERM_MASK] + ((yi + 1) and PERM_MASK)] + ((zi + 1) and PERM_MASK)]

        val x1 = lerp(grad(aaa, xf, yf, zf), grad(baa, xf - 1, yf, zf), u)
        val x2 = lerp(grad(aba, xf, yf - 1, zf), grad(bba, xf - 1, yf - 1, zf), u)
        val y1 = lerp(x1, x2, v)

        val x3 = lerp(grad(aab, xf, yf, zf - 1), grad(bab, xf - 1, yf, zf - 1), u)
        val x4 = lerp(grad(abb, xf, yf - 1, zf - 1), grad(bbb, xf - 1, yf - 1, zf - 1), u)
        val y2 = lerp(x3, x4, v)

        return lerp(y1, y2, w)
    }

    private fun fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)
    private fun lerp(a: Double, b: Double, t: Double): Double = a + t * (b - a)

    private fun grad(hash: Int, x: Double, y: Double, z: Double): Double {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        val uu = if (h and 1 == 0) u else -u
        val vv = if (h and 2 == 0) v else -v
        return uu + vv
    }

    private const val PERM_MASK = 255
    private val PERM_BASE = intArrayOf(
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99,
            37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
            57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27,
            166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102,
            143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116,
            188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126,
            255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152,
            2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113,
            224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,
            81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50,
            45, 127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61,
            156, 180
    )
    private val PERM = IntArray(512) { PERM_BASE[it and PERM_MASK] }

}
