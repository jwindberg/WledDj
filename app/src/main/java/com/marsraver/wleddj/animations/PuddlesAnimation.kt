package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.min
import kotlin.random.Random

/**
 * Puddles animation - Smooth vector ripples.
 */
class PuddlesAnimation : Animation {

    private var _palette: Palette = Palette.fromName(getDefaultPaletteName()) ?: Palette.DEFAULT
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    fun getDefaultPaletteName(): String = "Rainbow"

    // State
    private data class Ripple(
        var x: Float,
        var y: Float,
        var radius: Float,
        var maxRadius: Float,
        var alpha: Int, // 0-255
        var color: Int
    )

    private val ripples = mutableListOf<Ripple>()
    private var loudnessMeter: LoudnessMeter? = null
    private var lastPuddleTime: Long = 0L

    // Params
    private var paramSpeed: Int = 128
    private var paramIntensity: Int = 128
    
    // Tools
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private var isInit = false
    private fun init() {
        if (!isInit) {
            loudnessMeter = LoudnessMeter()
            isInit = true
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        init()
        
        val now = System.currentTimeMillis()
        val loudness: Float = (loudnessMeter?.getNormalizedLoudness() ?: 0).toFloat()
        
        // Spawn Logic
        // Reduce rate: larger interval.
        // paramSpeed 0..255. 
        // 0 -> 500ms (Slow)
        // 255 -> 100ms (Fast)
        val minInterval = 500 - (paramSpeed / 255f) * 400
        
        if (now - lastPuddleTime > minInterval) {
            // Trigger?
            // Increase threshold to reduce sensitivity
            if (loudness > 20f) { 
                spawnRipple(width, height, loudness)
                lastPuddleTime = now
            } else if (Random.nextFloat() < 0.005f) { // Occasional random drops (reduced from 0.02)
                 spawnRipple(width, height, 10f)
                 lastPuddleTime = now
            }
        }
        
        // Clear
        // Canvas is transparent? Or Black? 
        // Usually animations draw on black.
        // Usually animations draw on black.
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        
        // Update & Draw
        val iter = ripples.iterator()
        
        // Expansion Speed
        val speed = 1f + (paramSpeed / 255f) * 5f
        
        while (iter.hasNext()) {
            val r = iter.next()
            
            // Expand
            r.radius += speed
            
            // Fade
            // Alpha decays based on progress to maxRadius or just linear?
            // Let's decay linearly.
            r.alpha -= 2
            
            if (r.alpha <= 0 || r.radius > r.maxRadius) {
                iter.remove()
            } else {
                paint.color = r.color
                paint.alpha = r.alpha
                paint.strokeWidth = 3f * (1f - r.radius / r.maxRadius) + 1f // Thin out as it expands
                
                canvas.drawCircle(r.x, r.y, r.radius, paint)
            }
        }
    }
    
    private fun spawnRipple(w: Float, h: Float, intensity: Float) {
        val maxR = min(w, h) * (0.5f + (paramIntensity / 255f))
        
        val colorIndex = (System.currentTimeMillis() / 20).toInt() % 256
        val color = _palette.getInterpolatedInt(colorIndex)
        
        ripples.add(Ripple(
            x = Random.nextFloat() * w,
            y = Random.nextFloat() * h,
            radius = 0f,
            maxRadius = maxR, // Variable based on loudness could be cool too
            alpha = 255,
            color = color
        ))
    }

    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }
}
