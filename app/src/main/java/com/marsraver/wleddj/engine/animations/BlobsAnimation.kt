package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Floating Blobs animation
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palettes

class BlobsAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private class Blob {
        var x: Float = 0f
        var y: Float = 0f
        var speedX: Float = 0f
        var speedY: Float = 0f
        var radius: Float = 1f
        var grow: Boolean = false
        var color: Int = 0
    }

    private lateinit var blobs: Array<Blob>
    private var amount: Int = 0
    private val random = Random.Default
    private var lastColorChange: Long = 0
    private val MAX_BLOBS = 8
    private var startTimeNs: Long = 0L

    override fun onInit() {
        startTimeNs = System.nanoTime()
        lastColorChange = System.currentTimeMillis()
        currentPalette = Palettes.get("Rainbow")

        amount = min(MAX_BLOBS, (paramIntensity shr 5) + 1)
        blobs = Array(MAX_BLOBS) { Blob() }

        for (i in 0 until MAX_BLOBS) {
            val blob = blobs[i]
            val maxRadius = max(2, width / 4)
            blob.radius = random.nextFloat() * (maxRadius - 1) + 1

            val speedDiv = max(1, 256 - paramSpeed).toFloat()
            // Random speed 3..width/height
            blob.speedX = (random.nextInt(width.coerceAtLeast(4) - 3) + 3) / speedDiv
            blob.speedY = (random.nextInt(height.coerceAtLeast(4) - 3) + 3) / speedDiv

            blob.x = random.nextInt(width).toFloat()
            blob.y = random.nextInt(height).toFloat()
            blob.color = random.nextInt(256)
            blob.grow = blob.radius < 1.0f

            if (blob.speedX == 0f) blob.speedX = 1f
            if (blob.speedY == 0f) blob.speedY = 1f
        }
    }

    override fun update(now: Long): Boolean {
        // fadeToBlack
        val custom2 = 32
        val fadeAmount = (custom2 shr 3) + 1
        fadeToBlackBy(fadeAmount)

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastColorChange >= 2000) {
            for (i in 0 until amount) blobs[i].color = (blobs[i].color + 4) % 256
            lastColorChange = currentTime
        }

        for (i in 0 until amount) {
            val blob = blobs[i]
            val speedFactor = max(abs(blob.speedX), abs(blob.speedY))

            if (blob.grow) {
                blob.radius += speedFactor * 0.05f
                val maxRadius = min(width / 4.0f, 2.0f)
                if (blob.radius >= maxRadius) blob.grow = false
            } else {
                blob.radius -= speedFactor * 0.05f
                if (blob.radius < 1.0f) blob.grow = true
            }
            
            // hsv color from pseudo-palette index
            val color = getColorFromPalette(blob.color)
            val xPos = blob.x.roundToInt()
            val yPos = blob.y.roundToInt()

            if (blob.radius > 1.0f) {
                fillCircle(xPos, yPos, blob.radius.roundToInt(), color)
            } else {
                setPixelColor(xPos, yPos, color)
            }

            // Movement logic
            blob.x = when {
                blob.x + blob.radius >= width - 1 -> blob.x + blob.speedX * ((width - 1 - blob.x) / blob.radius + 0.005f)
                blob.x - blob.radius <= 0 -> blob.x + blob.speedX * (blob.x / blob.radius + 0.005f)
                else -> blob.x + blob.speedX
            }

            blob.y = when {
                blob.y + blob.radius >= height - 1 -> blob.y + blob.speedY * ((height - 1 - blob.y) / blob.radius + 0.005f)
                blob.y - blob.radius <= 0 -> blob.y + blob.speedY * (blob.y / blob.radius + 0.005f)
                else -> blob.y + blob.speedY
            }
            
            // Bounds check
             if (blob.x < 0.01f || blob.x > width - 1.01f) {
                 val speedDiv = max(1, 256 - paramSpeed).toFloat()
                 val dir = if (blob.x < 0.01f) 1 else -1
                 blob.speedX = dir * ((random.nextInt(width.coerceAtLeast(4) - 3) + 3) / speedDiv)
                 blob.x = if (dir == 1) 0.01f else width - 1.01f
            }
            if (blob.y < 0.01f || blob.y > height - 1.01f) {
                 val speedDiv = max(1, 256 - paramSpeed).toFloat()
                 val dir = if (blob.y < 0.01f) 1 else -1
                 blob.speedY = dir * ((random.nextInt(height.coerceAtLeast(4) - 3) + 3) / speedDiv)
                 blob.y = if (dir == 1) 0.01f else height - 1.01f
            }
        }

        // Blur
        val custom1 = 32
        val blurAmount = custom1 shr 2
        if (blurAmount > 0) blur2d(blurAmount)

        return true
    }
    
    // Fill circle helper with primitive additive blending
    private fun fillCircle(centerX: Int, centerY: Int, radius: Int, rgb: Int) {
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
                    // Additive blend locally
                    val current = getPixelColor(x, y)
                    val r = min(Color.red(current) + Color.red(rgb), 255)
                    val g = min(Color.green(current) + Color.green(rgb), 255)
                    val b = min(Color.blue(current) + Color.blue(rgb), 255)
                    setPixelColor(x, y, Color.rgb(r, g, b))
                }
            }
        }
    }
}
