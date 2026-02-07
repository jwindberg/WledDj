package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.marsraver.wleddj.engine.Animation
import kotlin.math.sin

class AuroraBorealisAnimation : Animation {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f 
        isAntiAlias = true
    }
    private var offset = 0f


    override fun supportsSpeed() = true
    
    private var speedMultiplier: Float = 1.0f // Default normal speed
    
    override fun setSpeed(speed: Float) {
        speedMultiplier = speed * 2f // Map 0.5 -> 1.0
    }
    
    override fun getSpeed() = speedMultiplier / 2f

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Standard speed
        offset += 0.05f * speedMultiplier
        
        // Draw 3 layers of waves
        drawWave(canvas, width, height, color = Color.GREEN, speed = 1f, yParams = Triple(0.3f, 50f, 0f))
        drawWave(canvas, width, height, color = Color.CYAN, speed = 0.7f, yParams = Triple(0.4f, 60f, 2f))
        drawWave(canvas, width, height, color = Color.MAGENTA, speed = 1.3f, yParams = Triple(0.5f, 40f, 4f))
    }

    private fun drawWave(canvas: Canvas, w: Float, h: Float, color: Int, speed: Float, yParams: Triple<Float, Float, Float>) {
        val (baseYFactor, amp, phaseShift) = yParams
        val path = Path()
        
        paint.color = color
        paint.alpha = 100 
        paint.strokeWidth = 30f 
        
        val points = 50
        val step = w / points
        
        path.moveTo(0f, h * baseYFactor + sin(offset * speed + phaseShift) * amp)
        
        for (i in 1..points) {
            val x = i * step
            val angle = offset * speed + (x / w) * 10f + phaseShift
            val y = h * baseYFactor + sin(angle) * amp + sin(angle * 0.5f) * amp * 0.5f
            path.lineTo(x, y)
        }
        
        canvas.drawPath(path, paint)
    }
    
    override fun destroy() {
        // No cleanup needed
    }
}
