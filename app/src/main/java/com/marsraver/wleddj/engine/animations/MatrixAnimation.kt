package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.random.Random
import kotlin.math.min

/**
 * Matrix animation - Falling code rain effect
 * Migrated to WledDj.
 */
class MatrixAnimation : BasePixelAnimation() {

    override fun supportsPrimaryColor(): Boolean = true // For Trail
    override fun supportsSecondaryColor(): Boolean = true // For Spawn/Head

    private lateinit var fallingCodes: BooleanArray  // Track which pixels are falling codes
    private val random = Random.Default
    
    private var custom1: Int = 128  // Trail size
    private var useCustomColors: Boolean = false
    private var step: Long = 0L
    private var startTimeNs: Long = 0L

    init {
        // Initialize defaults immediately so UI picks them up
        primaryColor = Color.rgb(27, 130, 39) // Trail (Dark Green)
        secondaryColor = Color.rgb(175, 255, 175) // Spawn (Light Green)
    }

    override fun onInit() {
        fallingCodes = BooleanArray(width * height)
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000

        val fade = MathUtils.map(custom1, 0, 255, 50, 250)
        val minRows = min(height, 150)
        val shiftAmount = MathUtils.map(minRows, 0, 150, 0, 3)
        val speedValue = (256 - paramSpeed) shr shiftAmount

        // Define colors
        val spawnColor = secondaryColor 
        val trailColor = primaryColor    

        var emptyScreen = true

        // Check if enough time has passed
        if (timeMs - step >= speedValue) {
            step = timeMs

            // Fade all pixels
            fadeToBlackBy(fade)

            // Move pixels one row down
            for (row in height - 1 downTo 0) {
                for (col in 0 until width) {
                    val idx = row * width + col
                    if (fallingCodes[idx]) {
                        // This is a falling code - create trail
                        setPixelColor(col, row, trailColor)

                        // Clear current position
                        fallingCodes[idx] = false

                        // Move down if not at bottom
                        if (row < height - 1) {
                            setPixelColor(col, row + 1, spawnColor)
                            fallingCodes[(row+1)*width + col] = true
                            emptyScreen = false
                        }
                    }
                }
            }

            // Spawn new falling code at top
            val shouldSpawn = random.nextInt(256) <= paramIntensity || emptyScreen
            if (shouldSpawn) {
                val spawnX = random.nextInt(width)
                setPixelColor(spawnX, 0, spawnColor)
                fallingCodes[spawnX] = true
            }
        }
        return true
    }
}
