package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.min
import kotlin.random.Random

/**
 * Space Ships animation - 2D spaceships
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palettes

class SpaceShipsAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true

    private var smear: Boolean = false  
    private var direction: Int = 0
    private var nextDirectionChange: Long = 0L
    private val random = Random.Default
    private var startTimeNs: Long = 0L

    override fun onInit() {
        startTimeNs = System.nanoTime()
        nextDirectionChange = 0L
        direction = random.nextInt(8)
        currentPalette = Palettes.get("Rainbow")
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000

        val tb = timeMs shr 12
        if (tb > nextDirectionChange) {
            var newDir = direction + (random.nextInt(3) - 1)
            if (newDir > 7) newDir = 0
            else if (newDir < 0) newDir = 7
            direction = newDir
            nextDirectionChange = tb + random.nextInt(4)
        }

        // Fade to black based on speed
        val fadeAmount = MathUtils.map(paramSpeed, 0, 255, 248, 16)
        fadeToBlackBy(fadeAmount)

        // Move buffer (SHIFT)
        // If speed is high, maybe move more? Original just called move(direction, 1) every frame.
        // We'll mimic that.
        move(direction, 1)


        for (i in 0 until 8) {
            val x = MathUtils.beatsin8(12 + i, 2, width - 3, timeMs)
            val y = MathUtils.beatsin8(15 + i, 2, height - 3, timeMs)
            val colorIndex = MathUtils.beatsin8(12 + i, 0, 255, timeMs)
            
            val color = getColorFromPalette(colorIndex)

            addPixelColor(x, y, color)

            if (smear) {
                addPixelColor(x + 1, y, color)
                addPixelColor(x - 1, y, color)
                addPixelColor(x, y + 1, color)
                addPixelColor(x, y - 1, color)
            }
        }

        val blurAmount = paramIntensity shr 3
        if (blurAmount > 0) blur2d(blurAmount)

        return true
    }

    private fun move(dir: Int, amount: Int) {
        if (amount <= 0) return

        val temp = pixels.clone() // Scratch copy

        for (x in 0 until width) {
            for (y in 0 until height) {
                // Determine source coordinates that shift TO here
                // If dir 0 is right (x+amount), then to get new value at x,y we read from x-amount?
                // Original: 
                /*
                 val (dx, dy) = when (dir) { ... } // delta
                 srcX = x - dx
                 srcY = y - dy
                 */
                
                // Assuming typical direction map:
                // 0: right (+1, 0)
                // 1: down-right (+1, +1)
                // 2: down (0, +1)
                // etc.
                
                var dx=0; var dy=0
                when (dir) {
                    0 -> { dx=amount; dy=0 }
                    1 -> { dx=amount; dy=amount }
                    2 -> { dx=0; dy=amount }
                    3 -> { dx=-amount; dy=amount }
                    4 -> { dx=-amount; dy=0 }
                    5 -> { dx=-amount; dy=-amount }
                    6 -> { dx=0; dy=-amount }
                    7 -> { dx=amount; dy=-amount }
                }

                val srcX = x - dx
                val srcY = y - dy

                if (srcX in 0 until width && srcY in 0 until height) {
                    setPixelColor(x, y, temp[srcY * width + srcX])
                } else {
                    setPixelColor(x, y, Color.BLACK)
                }
            }
        }
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
