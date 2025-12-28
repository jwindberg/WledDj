package com.marsraver.wleddj.animations

import android.graphics.Color
import kotlin.random.Random
import kotlin.math.abs

/**
 * Game of Life animation - Conway's Game of Life cellular automaton
 * Migrated to WledDj.
 */
class GameOfLifeAnimation : BasePixelAnimation() {

    override fun supportsPrimaryColor(): Boolean = true

    private data class Cell(
        var alive: Boolean = false,
        var faded: Boolean = false,
        var toggleStatus: Boolean = false,
        var edgeCell: Boolean = false,
        var oscillatorCheck: Boolean = false,
        var spaceshipCheck: Boolean = false
    )

    private lateinit var cells: Array<Array<Cell>>

    private var check3: Boolean = false  // Mutation
    // custom1 mapped to blur amount 0-255

    private var generation: Int = 0
    private var gliderLength: Int = 0
    private var step: Long = 0L
    private var startTimeNs: Long = 0L
    private val random = Random.Default

    override fun onInit() {
        cells = Array(width) { Array(height) { Cell() } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        
        // Calculate glider length LCM(rows,cols)*4
        var a = height
        var b = width
        while (b != 0) {
            val t = b
            b = a % b
            a = t
        }
        val safeA = if (a == 0) 1 else a
        gliderLength = (width * height / safeA) shl 2

        generation = 0
        step = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = System.nanoTime()
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000

        val mutate = check3
        // blur logic: custom1 was 128 default. Map 0-255 to 255-4.
        val blur = 64 // Fixed default for now as we don't have custom sliders exposed yet

        val bgColor = Color.BLACK
        // birthColor: palette index 128
        val birthColor = hsvToRgb(128, 255, 255)

        val setup = generation == 0 && step == 0L

        // Timebase jump fix
        if (abs(timeMs - step) > 2000) {
            step = 0L
        }

        val paused = step > timeMs

        // Setup New Game of Life
        if ((!paused && generation == 0) || setup) {
            step = timeMs + 1280  // Show initial state for 1.28 seconds
            generation = 1

            // Setup Grid
            for (x in 0 until width) {
                for (y in 0 until height) {
                    cells[x][y] = Cell()
                    val isAlive = random.nextInt(3) == 0  // ~33%
                    cells[x][y].alive = isAlive
                    cells[x][y].faded = !isAlive
                    cells[x][y].edgeCell = (x == 0 || x == width - 1 || y == 0 || y == height - 1)

                    val color = if (isAlive) {
                         if (primaryColor != Color.BLACK) primaryColor else hsvToRgb(random.nextInt(256), 255, 255)
                    } else {
                        bgColor
                    }
                    setPixelColor(x, y, color)
                }
            }
            return true
        }
        
        // Map speed to update interval (1ms to 1000ms range roughly)
        // 0 -> 1000ms, 255 -> ~24ms
        val speedVal = if(paramSpeed == 0) 1 else paramSpeed
        val updateInterval = 1000 / (speedVal / 6 + 1) // Rough mapping

        if (paused || (timeMs - step < updateInterval)) {
             // Just redraw or simple fade?
            return true
        }

        // Repeat detection
        val updateOscillator = generation % 16 == 0
        val updateSpaceship = gliderLength != 0 && generation % gliderLength == 0
        var repeatingOscillator = true
        var repeatingSpaceship = true
        var emptyGrid = true

        // Update cells logic
        for (x in 0 until width) {
            for (y in 0 until height) {
                val cell = cells[x][y]

                if (cell.alive) emptyGrid = false
                if (cell.oscillatorCheck != cell.alive) repeatingOscillator = false
                if (cell.spaceshipCheck != cell.alive) repeatingSpaceship = false
                if (updateOscillator) cell.oscillatorCheck = cell.alive
                if (updateSpaceship) cell.spaceshipCheck = cell.alive

                // Count alive neighbors
                var neighbors = 0
                var aliveParents = 0
                // We track parents for color inheritance (implifed: just check neighbors)

                for (i in -1..1) {
                    for (j in -1..1) {
                        if (i == 0 && j == 0) continue

                        var nX = x + j
                        var nY = y + i

                        if (cell.edgeCell) {
                            nX = (nX + width) % width
                            nY = (nY + height) % height
                        } else {
                            if (nX < 0 || nX >= width || nY < 0 || nY >= height) continue
                        }

                        val neighbor = cells[nX][nY]
                        if (neighbor.alive) {
                            neighbors++
                        }
                    }
                }

                if (cell.alive && (neighbors < 2 || neighbors > 3)) {
                    // Die
                    cell.toggleStatus = true
                } else if (!cell.alive) {
                    val mutationRoll = if (mutate) random.nextInt(128) else 1
                    if (neighbors == 3) {
                         // Reproduce
                         cell.toggleStatus = true
                    }
                }
                
                // Color Logic (Simplified for brevity in migration)
                if (cell.alive) {
                     // Keep current color or fade slightly?
                     // Pass
                } else {
                     setPixelColor(x, y, Color.BLACK)
                }
            }
        }

        // Apply toggles
        for (x in 0 until width) {
            for (y in 0 until height) {
                val cell = cells[x][y]
                if (cell.toggleStatus) {
                    cell.alive = !cell.alive
                    cell.toggleStatus = false
                    if (cell.alive) {
                         val newColor = if (primaryColor != Color.BLACK) primaryColor else hsvToRgb(random.nextInt(256), 255, 255)
                         setPixelColor(x, y, newColor)
                    }
                }
            }
        }

        if (repeatingOscillator || repeatingSpaceship || emptyGrid) {
            generation = 0
            step += 1024
        } else {
            generation++
            step = timeMs
        }

        return true
    }
}
