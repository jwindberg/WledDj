package com.marsraver.wleddj.engine.math

import kotlin.math.floor

/**
 * Noise utilities.
 * Implements Perlin Noise (Simplex-like) for 8-bit and 16-bit usage.
 * Modeled after FastLED's noise functions.
 */
object NoiseUtils {

    // Permutation table
    private val p = IntArray(512)

    init {
        // Standard Perlin permutation
        val permutation = intArrayOf(
            151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
            190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,88,237,149,56,87,174,20,
            125,136,171,168, 68,175,74,165,71,134,139,48,27,166,77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,
            55,46,245,40,244,102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,135,130,116,188,
            159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,5,202,38,147,118,126,255,82,85,212,207,206,59,227,
            47,16,58,17,182,189,28,42,223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,129,22,39,
            253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,251,34,242,193,238,210,144,12,191,179,162,241,
            81,51,145,235,249,14,239,107,49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,138,236,
            205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
        )
        for(i in 0 until 256) {
            p[i] = permutation[i]
            p[i+256] = permutation[i]
        }
    }

    /**
     * 8-bit noise function (0-255).
     * Scaled input: x, y, z are technically unrestricted but usually scaled coordinates.
     * Uses classic Perlin Noise implementation (fade + lerp).
     */
    fun inoise8(x: Int, y: Int, z: Int): Int {
        // Map input integers to 0-255 coordinate space roughly?
        // FastLED inoise8 treats inputs as 16.16 fixed point often, but here we take Int.
        // If x,y,z are large (like millis * scale), we use them directly.

        // Find unit cube that contains point
        val X = (x shr 8) and 255
        val Y = (y shr 8) and 255
        val Z = (z shr 8) and 255

        // Find relative x,y,z of point in cube (0-255)
        var xf = x and 255
        var yf = y and 255
        var zf = z and 255

        // Compute fade curves for each of x,y,z
        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        // Hash coordinates of the 8 cube corners
        val A = p[X] + Y
        val AA = p[A] + Z
        val AB = p[A + 1] + Z
        val B = p[X + 1] + Y
        val BA = p[B] + Z
        val BB = p[B + 1] + Z

        // Add blended results
        val res = lerp(w, lerp(v, lerp(u, grad(p[AA], xf, yf, zf),
                                     grad(p[BA], xf - 256, yf, zf)),
                             lerp(u, grad(p[AB], xf, yf - 256, zf),
                                     grad(p[BB], xf - 256, yf - 256, zf))),
                     lerp(v, lerp(u, grad(p[AA + 1], xf, yf, zf - 256),
                                     grad(p[BA + 1], xf - 256, yf, zf - 256)),
                             lerp(u, grad(p[AB + 1], xf, yf - 256, zf - 256),
                                     grad(p[BB + 1], xf - 256, yf - 256, zf - 256))))

        // Normalize result to 0-255. Perlin output is usually approx -1 to 1 range (scaled).
        // My grad implementation returns values roughly -256 to 256 range?
        // Let's adjust scale.
        // Standard Perlin returns -1..1.
        // Here we operate in integer space.

        // Simplification: returns 0-255.
        // Let's use simpler noise if this is too complex for int.
        // Or map result.

        // Actually, for FastLED compatibility, inoise8 expects x,y,z as scaled inputs (often 8-bit fraction).
        // Let's assume input is 16.16 logic where standard 1 unit = 256?
        // Yes, (x shr 8).

        return ((res + 128) shr 1).coerceIn(0, 255) // Rough mapping
    }

    private fun fade(t: Int): Int { 
        // 6t^5 - 15t^4 + 10t^3
        // scaled for 0-255 input?
        // t is 0..255 (fractional part)
        // This is tricky in pure Int without overflow.
        // Let's use double math for precision then convert back?
        val td = t / 255.0
        val r = td * td * td * (td * (td * 6 - 15) + 10)
        return (r * 255).toInt()
    }

    private fun lerp(t: Int, a: Int, b: Int): Int {
        // Linear Interpolation
        // t is 0..255
        return a + ((t * (b - a)) shr 8)
    }

    private fun grad(hash: Int, x: Int, y: Int, z: Int): Int {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        return ((if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v))
    }

    // 16-bit noise (wraps 8-bit scaled?)
    fun inoise16(x: Int, y: Int, z: Int): Int {
        val n = inoise8(x shr 8, y shr 8, z shr 8) // simplified
        return (n shl 8) + n // expand to 16 bit range 0-65535
    }

}
