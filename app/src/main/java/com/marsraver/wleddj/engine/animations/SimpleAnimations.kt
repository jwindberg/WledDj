package com.marsraver.wleddj.engine.animations
 
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.marsraver.wleddj.engine.Animation
import kotlin.random.Random



class BouncingBallAnimation(
    private var x: Float = 50f,
    private var y: Float = 50f,
    private val radius: Float = 30f
) : Animation {
    private var dx = 5f
    private var dy = 5f
    private val paint = Paint().apply { color = Color.RED }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
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

        canvas.drawCircle(x, y, radius, paint)
    }

    override fun onTouch(touchX: Float, touchY: Float): Boolean {
        // Simple distance check
        val dx = touchX - x
        val dy = touchY - y
        if (dx*dx + dy*dy < radius*radius) {
            // Apply impulse away from center (or random)
            this.dx = -this.dx * 1.5f
            this.dy = -this.dy * 1.5f
            return true
        }
        return false
    }
}


