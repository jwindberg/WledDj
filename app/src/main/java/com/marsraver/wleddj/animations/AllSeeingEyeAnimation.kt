package com.marsraver.wleddj.animations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
import com.marsraver.wleddj.R
import com.marsraver.wleddj.engine.Animation

class AllSeeingEyeAnimation(private val context: Context) : Animation {

    private var movie: Movie? = null
    // Time Logic
    private var lastFrameTime = 0L
    private var accumulatedTime = 0.0
    private var speed = 0.5f // Default mid-speed
    
    private val paint = Paint().apply { isFilterBitmap = true } // Smooth scaling

    init {
        try {
            val inputStream = context.resources.openRawResource(R.raw.all_seeing_eye)
            movie = Movie.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val m = movie ?: return

        val now = System.currentTimeMillis()
        if (lastFrameTime == 0L) lastFrameTime = now
        
        // Handle GIF duration
        var duration = m.duration()
        if (duration == 0) {
             duration = 1000 // Fallback default
        }
        
        // Speed Mapping: 0.0 -> 0x, 0.5 -> 1x, 1.0 -> 3x
        val multiplier = if (speed <= 0.5f) {
            speed * 2f // 0 to 1x
        } else {
            1f + (speed - 0.5f) * 4f // 1x to 3x
        }
        
        val dt = now - lastFrameTime
        lastFrameTime = now
        
        accumulatedTime += dt * multiplier
        
        val relTime = (accumulatedTime % duration).toInt()
        m.setTime(relTime)

        // Logic: Scale to Fit Center
        val saveCount = canvas.save()
        
        val gifW = m.width().toFloat()
        val gifH = m.height().toFloat()
        
        if (gifW > 0 && gifH > 0) {
            val scaleX = width / gifW
            val scaleY = height / gifH
            
            // Fit Center (ensure whole image is visible)
            val scale = minOf(scaleX, scaleY) 
            
            val dx = (width - gifW * scale) / 2f
            val dy = (height - gifH * scale) / 2f
            
            canvas.translate(dx, dy)
            canvas.scale(scale, scale)
            
            m.draw(canvas, 0f, 0f, paint)
        }
        
        canvas.restoreToCount(saveCount)
    }
    
    private fun minOf(a: Float, b: Float): Float = if (a < b) a else b

    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        this.speed = speed
    }
    override fun getSpeed(): Float = speed
}
