package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import kotlin.random.Random

/**
 * Matripix Animation - High-Res Digital Rain.
 * Replaces the old horizontal pixel shifter.
 * Falling trails of color using the active palette.
 */
class MatripixAnimation : Animation {

    private var _palette: Palette = Palettes.get("Rainbow") ?: Palettes.getDefault()
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // Params
    private var paramSpeed: Int = 128
    private var paramIntensity: Int = 128

    // Audio
    private var loudnessMeter: LoudnessMeter? = null
    
    // State
    private data class Droplet(
        var x: Float,
        var y: Float,
        var speed: Float,
        var length: Float,
        var width: Float,
        var colorIdx: Int // Index into palette 0-255
    )
    
    private val droplets = mutableListOf<Droplet>()
    private val paint = Paint().apply { isDither = true }
    private var initialized = false
    
    override fun draw(canvas: Canvas, width: Float, height: Float) {
        if (loudnessMeter == null) loudnessMeter = LoudnessMeter()
        if (!initialized) initDroplets(width, height)
        
        // Audio
        val loudness = loudnessMeter?.getNormalizedLoudness() ?: 0
        // Strong boost: 1.0 to 4.0x speed
        val audioBoost = 1.0f + (loudness / 1024.0f) * 3.0f
        
        // Pulse width: 1.0 to 2.5x
        val widthPulse = 1.0f + (loudness / 1024.0f) * 1.5f

        // Clear Black
        canvas.drawColor(Color.BLACK)
        
        // Base params
        val baseSpeed = 2.0f + (paramSpeed / 255f) * 10f
        
        // Iterate droplets
        droplets.forEach { d ->
            // Move
            d.y += d.speed * baseSpeed * audioBoost
            
            // Draw
            // We want a head (bright) and a tail (fading)
            // Linear Gradient is expensive per-frame per-object if we alloc it.
            // But for ~50 objects it's fine.
            
            val mainColor = _palette.getInterpolatedInt(d.colorIdx)
            // Head color is usually white-ish or bright version of mainColor
            // Let's just use mainColor for head, and transparent for tail.
            
            val tailY = d.y - d.length
            
            // Only draw if visible
            if (d.y > 0 && tailY < height) {
                // Gradient: Tail (Transparent) -> Head (Full Color)
                val shader = LinearGradient(
                    0f, tailY,
                    0f, d.y,
                    Color.TRANSPARENT,
                    mainColor,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                paint.strokeWidth = d.width * widthPulse // Pulse width
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND // Rounded head
                
                // Draw Line
                canvas.drawLine(d.x, tailY, d.x, d.y, paint)
                
                // Draw Bright Head Dot (optional, for "Leader" look)
                paint.shader = null
                paint.color = Color.WHITE
                paint.style = Paint.Style.FILL
                // canvas.drawCircle(d.x, d.y, d.width/2, paint) // Too bubbly?
            }
            
            // Reset if passed bottom
            if (tailY > height) {
                resetDroplet(d, width, height)
                d.y = -Random.nextFloat() * height * 0.5f // Start above
            }
        }
        
        // Maintain population count based on width
        val targetCount = (width / 20f).toInt().coerceIn(10, 200)
        if (droplets.size != targetCount) {
             initDroplets(width, height)
        }
    }
    
    private fun initDroplets(w: Float, h: Float) {
        droplets.clear()
        val count = (w / 20f).toInt().coerceIn(10, 200)
        for (i in 0 until count) {
            val d = Droplet(0f, 0f, 0f, 0f, 0f, 0)
            resetDroplet(d, w, h)
            d.y = Random.nextFloat() * h // Scatter initially
            droplets.add(d)
        }
        initialized = true
    }
    
    private fun resetDroplet(d: Droplet, w: Float, h: Float) {
        d.x = Random.nextFloat() * w
        d.y = -100f
        // Speed var
        d.speed = 0.5f + Random.nextFloat() * 1.5f
        
        // Appearance
        // Width: 2..10 px?
        d.width = 2f + Random.nextFloat() * 8f
        
        // Length: related to speed? faster = longer
        d.length = 20f + d.speed * 50f + Random.nextFloat() * 50f
        
        // Color: Pick from palette
        // Random usage of palette
        d.colorIdx = Random.nextInt(256)
    }
    
    override fun destroy() {
        loudnessMeter?.stop()
    }
}
