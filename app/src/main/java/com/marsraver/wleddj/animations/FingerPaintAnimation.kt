package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import com.marsraver.wleddj.engine.Animation

class FingerPaintAnimation : Animation {

    private var paintBitmap: Bitmap? = null
    private var paintCanvas: Canvas? = null

    private val drawPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var lastX = -1f
    private var lastY = -1f

    // Capabilities
    override fun supportsPrimaryColor(): Boolean = true
    override fun supportsSpeed(): Boolean = true

    // Speed parameter maps to brush size (thickness) as a ratio of min(width, height)
    private var brushSizeRatio = 0.05f // Default 5%
    
    override fun setSpeed(speed: Float) {
        // Map 0..1 to 0.01..0.20 range
        brushSizeRatio = 0.01f + speed * 0.19f
    }

    override fun getSpeed(): Float {
        return (brushSizeRatio - 0.01f) / 0.19f
    }

    private var _primaryColor: Int = Color.GREEN
    override var primaryColor: Int
        get() = _primaryColor
        set(value) {
            _primaryColor = value
        }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val w = width.toInt().coerceAtLeast(1)
        val h = height.toInt().coerceAtLeast(1)

        // Initialize or resize the persistent drawing bitmap if dimensions changed
        val currentBitmap = paintBitmap
        if (currentBitmap == null || currentBitmap.width != w || currentBitmap.height != h) {
            val newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val newCanvas = Canvas(newBitmap)
            newCanvas.drawColor(Color.TRANSPARENT)

            // If we had a previous bitmap, scale/draw it onto the new one so we don't lose the drawing
            if (currentBitmap != null) {
                val rect = android.graphics.Rect(0, 0, w, h)
                newCanvas.drawBitmap(currentBitmap, null, rect, Paint().apply { isFilterBitmap = true })
                currentBitmap.recycle()
            }
            paintBitmap = newBitmap
            paintCanvas = newCanvas
        }

        // Draw the persistent paint bitmap to the destination canvas
        paintBitmap?.let { bmp ->
            canvas.drawBitmap(bmp, 0f, 0f, null)
        }
    }

    override fun onTouch(x: Float, y: Float): Boolean {
        val pCanvas = paintCanvas ?: return false
        val bmp = paintBitmap ?: return false

        // Compute brush size based on min dimension of bitmap
        val minDim = kotlin.math.min(bmp.width, bmp.height).toFloat()
        val thickness = (minDim * brushSizeRatio).coerceAtLeast(2f)

        drawPaint.color = _primaryColor
        drawPaint.strokeWidth = thickness

        if (lastX < 0f || lastY < 0f) {
            // First point of a touch stroke, draw a dot (filled circle)
            val fillPaint = Paint(drawPaint).apply {
                style = Paint.Style.FILL
            }
            pCanvas.drawCircle(x, y, thickness / 2f, fillPaint)
        } else {
            // Consecutive points of a touch stroke, draw a line segment
            pCanvas.drawLine(lastX, lastY, x, y, drawPaint)
        }

        lastX = x
        lastY = y
        return true
    }

    override fun onInteractionEnd() {
        // Reset stroke tracking when touch lifts
        lastX = -1f
        lastY = -1f
    }

    override fun onCommand(cmd: String) {
        if (cmd == "STOP") {
            // Clear the canvas when STOP is pressed
            paintCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            lastX = -1f
            lastY = -1f
        }
    }

    override fun destroy() {
        paintBitmap?.recycle()
        paintBitmap = null
        paintCanvas = null
    }
}
