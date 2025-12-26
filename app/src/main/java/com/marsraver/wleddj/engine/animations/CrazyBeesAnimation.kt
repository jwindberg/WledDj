package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.min
import kotlin.math.abs
import kotlin.random.Random

/**
 * Crazy Bees animation - Bees flying to random targets.
 * Migrated to WledDj.
 */
class CrazyBeesAnimation : BasePixelAnimation() {

    private class Bee {
        var posX: Int = 0
        var posY: Int = 0
        var aimX: Int = 0
        var aimY: Int = 0
        var color: Int = 0
        var deltaX: Int = 0
        var deltaY: Int = 0
        var signX: Int = 1
        var signY: Int = 1
        var error: Int = 0

        fun setAim(w: Int, h: Int, random: Random) {
            aimX = random.nextInt(w)
            aimY = random.nextInt(h)
            // Always use random color
            color = android.graphics.Color.HSVToColor(floatArrayOf(random.nextFloat() * 360f, 1f, 1f))
            
            deltaX = abs(aimX - posX)
            deltaY = abs(aimY - posY)
            signX = if (posX < aimX) 1 else -1
            signY = if (posY < aimY) 1 else -1
            error = deltaX - deltaY
        }
    }

    private lateinit var bees: Array<Bee>
    private var numBees: Int = 0
    private var lastUpdateTime: Long = 0
    private var updateInterval: Long = 0

    private val random = Random.Default
    private val MAX_BEES = 5

    override fun onInit() {
        lastUpdateTime = 0

        numBees = min(MAX_BEES, (width * height) / 256 + 1)
        bees = Array(numBees) { Bee() }

        for (i in 0 until numBees) {
            val bee = bees[i]
            bee.posX = random.nextInt(width)
            bee.posY = random.nextInt(height)
            bee.setAim(width, height, random)
        }

        // speedFactor: (speed shr 4) + 1. 
        // WledFx used 16ms base * 16 / speedFactor?
        // Let's approximate update interval.
        val speedFactor = (paramSpeed shr 4) + 1
        updateInterval = 16_000_000L * 16L / speedFactor
    }

    override fun update(now: Long): Boolean {
        if (now - lastUpdateTime < updateInterval) return true
        lastUpdateTime = now

        val fadeAmount = 32
        val blurAmount = 10
        fadeToBlackBy(fadeAmount)
        blur2d(blurAmount)

        for (i in 0 until numBees) {
            val bee = bees[i]

            val flowerColor = bee.color
            // Draw flower (cross)
            addPixelColor(bee.aimX + 1, bee.aimY, flowerColor)
            addPixelColor(bee.aimX, bee.aimY + 1, flowerColor)
            addPixelColor(bee.aimX - 1, bee.aimY, flowerColor)
            addPixelColor(bee.aimX, bee.aimY - 1, flowerColor)

            if (bee.posX != bee.aimX || bee.posY != bee.aimY) {
                // Draw bee (slightly dimmer? Or just rely on standard)
                // WledFx used brightness 200 for bee.
                // We'll just use the color directly or slightly dimmed if needed.
                val beeColor = bee.color 
                setPixelColor(bee.posX, bee.posY, beeColor)

                val error2 = bee.error * 2
                if (error2 > -bee.deltaY) {
                    bee.error -= bee.deltaY
                    bee.posX += bee.signX
                }
                if (error2 < bee.deltaX) {
                    bee.error += bee.deltaX
                    bee.posY += bee.signY
                }
            } else {
                bee.setAim(width, height, random)
            }
        }
        return true
    }

    private fun addPixelColor(x: Int, y: Int, color: Int) {
        if (x in 0 until width && y in 0 until height) {
            val current = getPixelColor(x, y)
             val r = min(Color.red(current) + Color.red(color), 255)
             val g = min(Color.green(current) + Color.green(color), 255)
             val b = min(Color.blue(current) + Color.blue(color), 255)
             setPixelColor(x, y, Color.rgb(r, g, b))
        }
    }
}
