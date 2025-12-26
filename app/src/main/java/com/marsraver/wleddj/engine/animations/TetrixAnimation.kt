package com.marsraver.wleddj.engine.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.random.Random
import kotlin.math.min
import kotlin.math.max

/**
 * Tetrix animation - Tetris-like falling blocks
 * Migrated to WledDj.
 */
class TetrixAnimation : BasePixelAnimation() {

    private data class Tetris(
        var stack: Float = 0f,      
        var step: Int = 0,          
        var speed: Float = 0f,      
        var pos: Float = 0f,        
        var col: Int = 0,           
        var brick: Int = 0,         
        var stackColors: MutableList<Int> = mutableListOf(),  
        var initialized: Boolean = false,  
        var startDelay: Int = 0  
    )

    private var oneColor: Boolean = false  
    private val drops = mutableListOf<Tetris>()
    private var lastUpdateNs: Long = 0L

    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        drops.clear()
        for (x in 0 until width) {
            val randomDelay = Random.nextInt(0, 500)
            drops.add(Tetris(stackColors = mutableListOf(), initialized = false, startDelay = randomDelay))
        }
        lastUpdateNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (width <= 0 || height <= 0) return true
        if (drops.isEmpty()) onInit() // Re-init if empty (e.g. resize)

        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
            return true
        }

        val deltaNs = now - lastUpdateNs
        lastUpdateNs = now
        val deltaMs = deltaNs / 1_000_000.0

        for (stripNr in 0 until min(width, drops.size)) {
            runStrip(stripNr, drops[stripNr], deltaMs)
        }
        return true
    }

    private fun runStrip(stripNr: Int, drop: Tetris, deltaMs: Double) {
        val stripLength = height
        val FADE_TIME_MS = 2000.0
        val FRAMETIME_MS = 16.67

        if (!drop.initialized) {
            drop.stack = 0f
            drop.stackColors.clear()
            drop.step = 0
            drop.initialized = true
            if (oneColor) drop.col = 0
        }

        if (drop.startDelay > 0) {
            drop.startDelay--
            for (y in 0 until stripLength) setPixelColor(stripNr, y, Color.BLACK)
            return
        }

        if (drop.step == 0) {
            val speedValue = if (paramSpeed > 0) paramSpeed else Random.nextInt(1, 255)
            val timeForFullDrop = MathUtils.map(speedValue, 1, 255, 5000, 250).toDouble()
            drop.speed = ((stripLength * FRAMETIME_MS) / timeForFullDrop).toFloat()
            drop.pos = -drop.brick.toFloat()

            if (!oneColor) drop.col = Random.nextInt(0, 16) shl 4

            drop.step = 1
            drop.brick = if (paramIntensity > 0) {
                ((paramIntensity shr 5) + 1) * (1 + (stripLength shr 6))
            } else {
                Random.nextInt(1, 5) * (1 + (stripLength shr 6))
            }
        }

        if (drop.step == 1) {
             val randomValue = Random.nextInt(0, 256)
             if (randomValue >= 64) drop.step = 2
        }

        if (drop.step == 2) {
             val stackTop = stripLength - drop.stack.toInt()
             
             drop.pos += (drop.speed * (deltaMs / FRAMETIME_MS)).toFloat()
             val newBrickBottom = drop.pos + drop.brick
             
             // Check collision with stack
             if (newBrickBottom >= stackTop) {
                 // Hit stack
                 drop.pos = stackTop - drop.brick.toFloat()
                 val stackHeight = drop.stack.toInt() + drop.brick
                 val newStackTop = stripLength - stackHeight



                 // Determine color: If Primary Color is not default white/black? No, user explicitly sets it.
                 // Actually, let's use primaryColor if oneColor is true.
                 // We can auto-enable oneColor if primaryColor is modified? Hard to detect change here.
                 // Instead, let's mix the logic.
                 
                 // For now, let's stick to using primaryColor ONLY if we decide to override.
                 // The easiest path is to use primaryColor as the brick color if `oneColor` is true, 
                 // and maybe force oneColor=true if we detect support?
                 
                 // Let's just use random HSV for now as per original code unless I refactor `oneColor`.
                 val brickColor = getColorFromPalette(drop.col)
                 for (i in 0 until drop.brick) drop.stackColors.add(brickColor)

                 if (drop.stackColors.size > stackHeight) {
                     // drop.stackColors = drop.stackColors.takeLast(stackHeight).toMutableList()
                     // Optimize: remove first
                     while(drop.stackColors.size > stackHeight) drop.stackColors.removeAt(0)
                 }

                 // Redraw full strip
                 for (y in 0 until stripLength) {
                     val color = when {
                         y >= newStackTop -> {
                             val stackIndex = y - newStackTop
                             if (stackIndex < drop.stackColors.size) drop.stackColors[stackIndex]
                             else getColorFromPalette((drop.col + stackIndex * 16) % 256)
                         }
                         else -> Color.BLACK
                     }
                     setPixelColor(stripNr, y, color)
                 }

                 drop.step = 0
                 drop.stack += drop.brick
                 if (drop.stack >= stripLength) {
                     drop.step = (System.currentTimeMillis() + FADE_TIME_MS).toInt()
                 }
                 
             } else {
                 // Still falling
                 val brickStart = drop.pos.toInt().coerceAtLeast(0)
                 val brickEnd = (drop.pos + drop.brick).toInt().coerceAtMost(stackTop)

                 for (y in 0 until stripLength) {
                     val color = when {
                         y >= stackTop -> {
                             val stackIndex = y - stackTop
                             if (stackIndex < drop.stackColors.size) drop.stackColors[stackIndex]
                             else getColorFromPalette((drop.col + stackIndex * 16) % 256)
                         }
                         y >= brickStart && y < brickEnd -> getColorFromPalette(drop.col)
                         else -> Color.BLACK
                     }
                     setPixelColor(stripNr, y, color)
                 }
             }
        }

        // Fading out full stack
        if (drop.step > 2) {
            drop.brick = 0
            val fadeEndTime = drop.step.toLong()
            val currentTime = System.currentTimeMillis()

            if (currentTime < fadeEndTime) {
                 // Fade logic: blend to black
                 // Since we redraw every frame, we can just dim existing colors?
                 // But we don't persist well in this loop structure.
                 // Just fill black with transparency? No.
                 // We need to dim the stack colors in the model?
                 // To simplify: we just dim the drawn pixels in buffer.
                 for (y in 0 until stripLength) {
                    val current = getPixelColor(stripNr, y)
                    val dim = fadeColor2(current, 25) // Fade out by 25
                    setPixelColor(stripNr, y, dim)
                }
            } else {
                drop.stack = 0f
                drop.stackColors.clear()
                drop.step = 0
                if (oneColor) drop.col = (drop.col + 8) % 256
            }
        }
    }
    
    private fun fadeColor2(color: Int, amount: Int): Int {
        val scale = (255 - amount).coerceAtLeast(0) / 255.0
        return Color.rgb(
           (Color.red(color) * scale).toInt(),
           (Color.green(color) * scale).toInt(),
           (Color.blue(color) * scale).toInt()
        )
    }
}
