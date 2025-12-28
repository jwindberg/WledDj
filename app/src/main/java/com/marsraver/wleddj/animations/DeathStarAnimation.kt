package com.marsraver.wleddj.animations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
import com.marsraver.wleddj.R
import com.marsraver.wleddj.engine.Animation

class DeathStarAnimation(private val context: Context) : Animation {

    private var movie: Movie? = null
    private var startTime = 0L
    private val paint = Paint().apply { isFilterBitmap = true } // Smooth scaling

    init {
        try {
            val inputStream = context.resources.openRawResource(R.raw.death_star_run)
            movie = Movie.decodeStream(inputStream)
            // inputStream.close() // Movie might need stream? Usually decodeStream reads fully or headers.
            // Android Docs say: decodeStream returns a Movie.
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val m = movie ?: return

        val now = System.currentTimeMillis()
        if (startTime == 0L) startTime = now
        
        val duration = m.duration()
        if (duration == 0) {
             // Single frame GIF or invalid duration
             m.setTime(0)
        } else {
             val relTime = ((now - startTime) % duration).toInt()
             m.setTime(relTime)
        }

        // Logic: Scale to Fit Center
        val saveCount = canvas.save()
        
        val gifW = m.width().toFloat()
        val gifH = m.height().toFloat()
        
        if (gifW > 0 && gifH > 0) {
            val scaleX = width / gifW
            val scaleY = height / gifH
            
            // Fit Center
            val scale = minOf(scaleX, scaleY) // or maxOf to Fill? User wants visibility. Fit Center.
            // Actually, "Fill" (maxOf) usually looks better for LED matrix if we want to cover gaps.
            // But "Fit" ensures whole image checks out.
            // Let's stick to Fit Center (minOf) as per previous attempts.
            // Wait, previous attempts had `maxOf(w, h)` in denominator -> that was "Scale Down to Target".
            
            // Standard "Aspect Fit":
            // scale = min(dstW/srcW, dstH/srcH)
            
            // Center
            val dx = (width - gifW * scale) / 2f
            val dy = (height - gifH * scale) / 2f
            
            canvas.translate(dx, dy)
            canvas.scale(scale, scale)
            
            m.draw(canvas, 0f, 0f, paint)
        }
        
        canvas.restoreToCount(saveCount)
    }
    
    private fun minOf(a: Float, b: Float): Float = if (a < b) a else b
}
