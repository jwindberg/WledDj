package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation

class BouncingBallAnimation : Animation {
    
    private var x: Float = 50f
    private var y: Float = 50f
    private val radius: Float = 30f
    
    // Capability
    override fun supportsPrimaryColor(): Boolean = true
    
    // State
    private var _primaryColor: Int = Color.RED
    override var primaryColor: Int
        get() = _primaryColor
        set(value) {
            _primaryColor = value
            paint.color = value
        }

    // Unused
    override var secondaryColor: Int = Color.BLACK
    override fun supportsSecondaryColor(): Boolean = false
    override var currentPalette: com.marsraver.wleddj.engine.color.Palette? = null
    override fun supportsPalette(): Boolean = false


    private var dx = 5f
    private var dy = 5f
    private val paint = Paint().apply { color = _primaryColor }
    
    init {
        // Ensure UI picks up default
        primaryColor = Color.RED
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        if (!isDragging) {
            x += dx
            y += dy

            if (x - radius < 0) {
                x = radius
                dx = Math.abs(dx) // Ensure moving right
            } else if (x + radius > width) {
                x = width - radius
                dx = -Math.abs(dx) // Ensure moving left
            }

            if (y - radius < 0) {
                y = radius
                dy = Math.abs(dy) // Ensure moving down
            } else if (y + radius > height) {
                y = height - radius
                dy = -Math.abs(dy) // Ensure moving up
            }
        }

        canvas.drawCircle(x, y, radius, paint)
    }

    // Velocity Tracking
    private data class HistoryPoint(val time: Long, val x: Float, val y: Float)
    private val velocityHistory = java.util.ArrayDeque<HistoryPoint>()
    private val MAX_HISTORY_MS = 200

    // Drag State
    private var isDragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    override fun onTouch(touchX: Float, touchY: Float): Boolean {
        val now = System.currentTimeMillis()

        if (isDragging) {
            // Updated Position
            x = touchX + dragOffsetX
            y = touchY + dragOffsetY
            
            // Add to history
            velocityHistory.addLast(HistoryPoint(now, x, y))
            
            // Prune old history
            while (!velocityHistory.isEmpty() && now - velocityHistory.peekFirst()!!.time > MAX_HISTORY_MS) {
                velocityHistory.removeFirst()
            }
            return true
        }

        // Hit Test
        val curDx = touchX - x
        val curDy = touchY - y
        val distSq = curDx*curDx + curDy*curDy

        if (distSq < radius*radius) {
            // INSIDE: Start Drag
            isDragging = true
            dx = 0f
            dy = 0f
            
            // Calculate Offset to prevent snapping
            dragOffsetX = x - touchX
            dragOffsetY = y - touchY
            
            velocityHistory.clear()
            velocityHistory.add(HistoryPoint(now, x, y))
            return true
        } else {
            // OUTSIDE: Repel
            // Only Repel if within proximity (e.g. 3x radius)
            if (distSq > (radius * 3) * (radius * 3)) {
                return false
            }

            // "Strike where the touch is and reverse, but maintain speed"
            var dirX = x - touchX
            var dirY = y - touchY
            
            val len = Math.sqrt((dirX*dirX + dirY*dirY).toDouble()).toFloat()
            if (len > 0.001f) {
                dirX /= len
                dirY /= len
                
                // Current speed magnitude
                val speed = Math.sqrt((dx*dx + dy*dy).toDouble()).toFloat()
                val targetSpeed = if (speed < 0.1f) 5f else speed
                
                dx = dirX * targetSpeed
                dy = dirY * targetSpeed
            }
            return true
        }
    }

    override fun onTransform(
        panX: Float,
        panY: Float,
        zoom: Float,
        rotation: Float
    ): Boolean {
        // Ignored. We use onTouch for absolute positioning which handles scaling correctly.
        return false 
    }

    override fun onInteractionEnd() {
        if (isDragging) {
            // ... (existing logic) ...
            isDragging = false
            
            // Calculate Velocity from History
            if (velocityHistory.size >= 2) {
                // ... (existing logic) ...
                val last = velocityHistory.peekLast()!!
                val first = velocityHistory.peekFirst()!! // Oldest in window
                
                val dt = (last.time - first.time).toFloat()
                if (dt > 10) { // Avoid div by zero or tiny timestamps
                    val distXx = last.x - first.x
                    val distYy = last.y - first.y
                    
                    // Pixels per ms
                    val velX = distXx / dt
                    val velY = distYy / dt
                    
                    // Convert to Pixels per Frame 
                    // Increased scale for punchier throws
                    val FLING_SCALE = 40f 
                    
                    dx = velX * FLING_SCALE
                    dy = velY * FLING_SCALE
                } else {
                     dx = 0f; dy = 0f
                }
            } else {
                dx = 0f; dy = 0f
            }
            
            // Limit max speed?
            val maxSpeed = 50f
            dx = dx.coerceIn(-maxSpeed, maxSpeed)
            dy = dy.coerceIn(-maxSpeed, maxSpeed)

            velocityHistory.clear()
        }
    }
    
    override fun onCommand(cmd: String) {
        if (cmd == "STOP") {
            dx = 0f
            dy = 0f
            velocityHistory.clear()
            isDragging = false // Force release if dragging
        }
    }
}
