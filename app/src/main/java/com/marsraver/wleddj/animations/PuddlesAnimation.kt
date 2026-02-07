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

    private var _palette: Palette = Palette.RAINBOW
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true



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

    // Speed Control
    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        // Map 0.0-1.0 to 0-255
        paramSpeed = (speed * 255f).toInt().coerceIn(0, 255)
    }
    override fun getSpeed(): Float = paramSpeed / 255f
    
    // Palette Control is already present (supportsPalette = true)

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
        
        // --- Spawn Logic ---
        // Speed controls Frequency (Spawn Rate).
        // 0.0 (Slow) -> 2000ms interval
        // 1.0 (Fast) -> 100ms interval
        val normalizedSpeed = paramSpeed / 255f
        val minInterval = 2000L - (normalizedSpeed * 1900L)
        
        if (now - lastPuddleTime > minInterval) {
            // Audio Trigger
            if (loudness > 20f) { 
                spawnRipple(width, height, loudness)
                lastPuddleTime = now
            } 
            // Auto Trigger (Default Rain)
            // Probability depends on speed too? 
            // No, just interval. If fixed interval passed, we spawn.
            // But we want it random.
            else {
                 spawnRipple(width, height, 10f)
                 lastPuddleTime = now
            }
        }
        
        // Clear Background (Black)
        canvas.drawColor(Color.BLACK) 
        
        // --- Update & Draw ---
        val iter = ripples.iterator()
        
        // Expansion Speed (Visual)
        // Fixed base speed + slight variance? 
        // Or controlled by Intensity? (If we had it).
        // Let's make expansion speed fixed or slightly growing.
        val expansionRate = 2f + (normalizedSpeed * 3f) 
        
        while (iter.hasNext()) {
            val r = iter.next()
            
            // Expand
            r.radius += expansionRate
            
            // Fade
            // Alpha decays based on size?
            // Decay faster if larger.
            val progress = r.radius / r.maxRadius
            r.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
            
            if (r.alpha <= 0 || r.radius > r.maxRadius) {
                iter.remove()
            } else {
                paint.color = r.color
                paint.alpha = r.alpha
                // Stroke gets thinner as it expands
                paint.strokeWidth = 10f * (1f - progress).coerceAtLeast(0.1f)
                
                canvas.drawCircle(r.x, r.y, r.radius, paint)
            }
        }
    }
    
    private fun spawnRipple(w: Float, h: Float, intensity: Float) {
        val maxR = min(w, h) * 0.6f // Max size 60% of screen min dim
        
        // Color from Palette
        // Use random index? Or cycle?
        // Random is better for "Puddles/Rain".
        val color = if (_palette != null) {
            _palette.getInterpolatedInt(Random.nextInt(256))
        } else {
             Color.BLUE // Fallback
        }
        
        ripples.add(Ripple(
            x = Random.nextFloat() * w,
            y = Random.nextFloat() * h,
            radius = 0f,
            maxRadius = maxR,
            alpha = 255,
            color = color
        ))
    }

    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }
}
