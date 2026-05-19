package com.marsraver.wleddj.animations

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

class PacManAnimation : BasePixelAnimation() {

    private var pacmanX = 0f
    private var pacmanY = 0f
    
    private var targetDot: Dot? = null
    
    private val ghosts = mutableListOf<Ghost>()
    private val dots = mutableListOf<Dot>()
    
    private var lastUpdate: Long = 0

    override fun supportsSpeed(): Boolean = true

    override fun onInit() {
        pacmanX = width / 2f
        pacmanY = height / 2f
        lastUpdate = System.currentTimeMillis()
        
        resetLevel()
    }

    private fun resetLevel() {
        pacmanX = width / 2f
        pacmanY = height / 2f
        
        ghosts.clear()
        ghosts.add(Ghost(0f, 0f, Color.RED)) // Blinky
        ghosts.add(Ghost(width.toFloat(), 0f, Color.CYAN)) // Inky
        ghosts.add(Ghost(0f, height.toFloat(), Color.MAGENTA)) // Pinky
        
        dots.clear()
        for (y in 2 until height step 6) {
            for (x in 2 until width step 6) {
                // Don't put dots too close to center where pacman starts
                if (abs(x - width/2f) > 4 || abs(y - height/2f) > 4) {
                    dots.add(Dot(x.toFloat(), y.toFloat()))
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        val dtMillis = now - lastUpdate
        if (dtMillis < 16) return false
        lastUpdate = now
        
        val dt = (dtMillis / 1000f) * (paramSpeed / 128f).coerceAtLeast(0.1f)
        val pacmanSpeed = 25f * dt
        val ghostSpeed = pacmanSpeed * 0.4f // Ghosts are much slower since there are no walls
        
        // Find nearest dot for PacMan to chase
        if (dots.isEmpty()) {
            resetLevel()
            return true
        }
        
        targetDot = dots.minByOrNull { dist(pacmanX, pacmanY, it.x, it.y) }
        
        // Orthogonal movement for PacMan
        targetDot?.let { dot ->
            val dx = dot.x - pacmanX
            val dy = dot.y - pacmanY
            
            if (abs(dx) > abs(dy)) {
                pacmanX += sign(dx) * pacmanSpeed
            } else {
                pacmanY += sign(dy) * pacmanSpeed
            }
        }
        
        // Eat dots
        dots.removeAll { dist(it.x, it.y, pacmanX, pacmanY) < 3f }
        
        // Move Ghosts (Orthogonal Chasing)
        for (ghost in ghosts) {
            val dx = pacmanX - ghost.x
            val dy = pacmanY - ghost.y
            
            // Add a tiny bit of random AI so they don't get perfectly stuck in diagonal loops
            if (abs(dx) > abs(dy)) {
                ghost.x += sign(dx) * ghostSpeed
            } else {
                ghost.y += sign(dy) * ghostSpeed
            }
            
            // Check collision with PacMan
            if (dist(ghost.x, ghost.y, pacmanX, pacmanY) < 3.5f) {
                // Eaten! Just reset the level without flashing
                resetLevel()
                return true
            }
        }
        
        // Render
        fadeToBlackBy(120) // Fast fade
        
        // Draw Dots
        for (dot in dots) {
            if (dot.x.toInt() in 0 until width && dot.y.toInt() in 0 until height) {
                drawBlob(dot.x, dot.y, Color.WHITE, 2)
            }
        }
        
        // Draw Ghosts
        for (ghost in ghosts) {
            drawBlob(ghost.x, ghost.y, ghost.color, 4)
        }
        
        // Draw PacMan
        drawBlob(pacmanX, pacmanY, Color.YELLOW, 4)
        
        return true
    }
    
    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx*dx + dy*dy)
    }
    

    
    data class Ghost(var x: Float, var y: Float, val color: Int)
    data class Dot(val x: Float, val y: Float)
}
