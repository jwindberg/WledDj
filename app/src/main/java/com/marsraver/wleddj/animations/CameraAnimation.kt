package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.marsraver.wleddj.camera.CameraManager
import com.marsraver.wleddj.engine.Animation

class CameraAnimation : Animation {
    private var cameraFacing: String = "back"
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val bmp = CameraManager.getLatestFrame()
        if (bmp != null) {
            val destRect = RectF(0f, 0f, width, height)
            canvas.drawBitmap(bmp, null, destRect, paint)
        } else {
            // Sleek retro-futuristic camera lens placeholder
            val placeholderPaint = Paint().apply {
                color = android.graphics.Color.argb(255, 100, 100, 100)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(0f, 0f, width, height, placeholderPaint)
            canvas.drawLine(0f, 0f, width, height, placeholderPaint)
            canvas.drawLine(width, 0f, 0f, height, placeholderPaint)

            val cx = width / 2f
            val cy = height / 2f
            val r = Math.min(width, height) * 0.2f

            if (r > 0) {
                val ringPaint = Paint().apply {
                    color = android.graphics.Color.argb(255, 120, 120, 120)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawCircle(cx, cy, r, ringPaint)

                val fillPaint = Paint().apply {
                    color = android.graphics.Color.argb(255, 60, 60, 60)
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, r * 0.6f, fillPaint)
            }
        }
    }

    override fun supportsText(): Boolean = true

    override fun setText(text: String) {
        cameraFacing = text
    }

    override fun getText(): String {
        return cameraFacing
    }

    override fun destroy() {
        // Handled cleanly by CameraManager and lifecycle bindings
    }
}
