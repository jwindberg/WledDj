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

    private var scaleFactor = 1.0f

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Init center if not set
        if (targetX < 0) {
            targetX = width / 2f
            targetY = height / 2f
        }
        
        // Dynamic radius with pinch zoom support
        // Base: 7.5% of screen. Scaled by user pinch (0.1x to 10x range)
        val baseRadius = kotlin.math.min(width, height) * 0.075f
        val radius = (baseRadius * scaleFactor).coerceAtLeast(baseRadius * 0.1f)
        
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

    private var userHasInteracted = false

    override fun onTouch(x: Float, y: Float): Boolean {
        // First Touch: Allow grabbing from anywhere (Snap to finger)
        if (!userHasInteracted) {
            userHasInteracted = true
            targetX = x
            targetY = y
            return true
        }

        // Visual Hit Test: Only consume if touching the "Light Beam"
        if (targetX >= 0) {
             val dx = x - targetX
             val dy = y - targetY
             val dist = kotlin.math.sqrt(dx*dx + dy*dy)
             
             // Dynamic Hit Threshold
             val hitRadius = 100f * scaleFactor // Heuristic
             if (dist > hitRadius) {
                 return false // Too far! Let it fall through to Tron.
             }
        }
        
        // Direct tap moves it (if hit)
        targetX = x
        targetY = y
        return true
    }

    override fun ignoresBounds(): Boolean = true

    override fun onTransform(
        panX: Float,
        panY: Float,
        zoom: Float,
        rotation: Float
    ): Boolean {
        // Always Apply Zoom (so it responds to even small pinches, especially in Layout Mode)
        scaleFactor = (scaleFactor * zoom).coerceIn(0.5f, 10.0f)

        // Only Pan if NOT zooming significantly (to prevent hopping)
        if (kotlin.math.abs(zoom - 1f) < 0.002f) {
             if (targetX < 0) {
                 // First move before draw? Use safe defaults but accumulation will be off center
                 // Ideally drawing happens first. If not, just start accumulating from 0?
                 // But 0,0 is Top Left. User gesture is somewhere.
                 // We can't know Width/Height here.
                 // Best effort: Don't vanish.
             }
             if (targetX >= 0) { 
                targetX += panX
                targetY += panY
             }
        }
        
        return true
    }
}
