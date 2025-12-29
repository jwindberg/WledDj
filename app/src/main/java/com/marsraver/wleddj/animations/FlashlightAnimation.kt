package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation

class FlashlightAnimation : Animation {
    private var targetX = -1f
    private var targetY = -1f
    
    // Default to Yellow (classic flashlight)
    private var _primaryColor: Int = Color.YELLOW
    
    override var primaryColor: Int
        get() = _primaryColor
        set(value) { _primaryColor = value }

    override fun supportsPrimaryColor(): Boolean = true

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Init center if not set
        if (targetX < 0) {
            targetX = width / 2f
            targetY = height / 2f
        }
        
        // Fixed radius (Ball size approx = 30f)
        val radius = 30f 
        
        // Glowing Spot using Primary Color
        // RadialGradient: Center (Color), Edge (Transparent)
        val shader = android.graphics.RadialGradient(
            targetX, targetY,
            radius,
            intArrayOf(_primaryColor, Color.TRANSPARENT),
            floatArrayOf(0.2f, 1.0f),
            android.graphics.Shader.TileMode.CLAMP
        )
        
        paint.shader = shader
        
        // Draw full rect to allow spill
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    override fun onTouch(x: Float, y: Float): Boolean {
        targetX = x
        targetY = y
        return true
    }
}
