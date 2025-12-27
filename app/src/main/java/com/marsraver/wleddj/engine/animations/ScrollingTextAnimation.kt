package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.marsraver.wleddj.engine.Animation

class ScrollingTextAnimation : Animation {

    override fun supportsPrimaryColor(): Boolean = true
    override fun supportsText(): Boolean = true

    private var _text = "HELLO WLED"
    private var textWidth = 0f
    private var scrollX = 0f
    private val scrollSpeed = 5f // Pixels per frame

    private var _primaryColor = Color.GREEN // Default matrix green
    override var primaryColor: Int
        get() = _primaryColor
        set(value) { _primaryColor = value }

    private val paint = Paint().apply {
        color = _primaryColor
        textSize = 100f // Will scale dynamically
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    override fun setText(text: String) {
        if (text.isBlank()) return
        _text = text
        // Reset scroll when text changes to ensure visibility
        refreshMetrics()
    }
    
    override fun getText(): String = _text

    private fun refreshMetrics() {
        textWidth = paint.measureText(_text)
        // If we change text, maybe reset scrollX to screen width? 
        // We'll handle this in draw to be safe with unknown 'width'
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Dynamic sizing: Text should be ~80% of screen height
        val targetSize = height * 0.8f
        if (paint.textSize != targetSize) {
            paint.textSize = targetSize
            refreshMetrics()
            // Initial positioning: Start off-screen right
            if (scrollX == 0f) scrollX = width
        }

        paint.color = _primaryColor

        // Update position
        scrollX -= scrollSpeed
        
        // Loop logic: If text goes fully off-screen left, wrap to right
        if (scrollX < -textWidth) {
            scrollX = width
        }

        // Draw
        // Center vertically: (height / 2) - ((descent + ascent) / 2)
        val yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        
        canvas.drawText(_text, scrollX, yPos, paint)
    }
}
