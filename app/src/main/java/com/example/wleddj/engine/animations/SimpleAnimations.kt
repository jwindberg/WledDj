package com.example.wleddj.engine.animations
 
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.wleddj.engine.Animation
import kotlin.random.Random

class StationaryBallAnimation : Animation {
    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val radius = minOf(width, height) * 0.4f
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
    }
}

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

        if (x - radius < 0 || x + radius > width) dx = -dx
        if (y - radius < 0 || y + radius > height) dy = -dy

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

class RandomRectsAnimation : Animation {
    private val paint = Paint()

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        paint.color = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
        val x = Random.nextFloat() * width
        val y = Random.nextFloat() * height
        canvas.drawRect(x, y, x + 50, y + 50, paint)
    }
}
