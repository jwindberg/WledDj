package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import kotlin.math.cos
import kotlin.math.sin

class SonarAnimation : Animation {

    // Use High-Res FFT for Precision Bass (64 bands = ~350Hz per band)
    private val fftMeter = com.marsraver.wleddj.engine.audio.FftMeter(bands = 64)
    
    // Fallback loudness for line thickness
    private val loudnessMeter = LoudnessMeter()
    
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    
    // Fill paint for the sweep fade
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var _palette: Palette = Palettes.get("Rainbow") ?: Palettes.getDefault()
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    // State
    private var scanAngle = 0f
    private val scanSpeed = 2.0f // Degrees per frame
    
    private class Ripple {
        var radius = 0f
        var maxRadius = 0f
        var color = 0
        var active = false
    }
    
    private val ripples = Array(10) { Ripple() }
    private var lastBass = 0f
    private var cooldown = 0

    override fun supportsPrimaryColor(): Boolean = false
    override fun supportsSecondaryColor(): Boolean = false
    override fun supportsPalette(): Boolean = true

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val maxDim = Math.max(width, height)
        
        // 1. Audio Analysis (Beat Match)
        val bands = fftMeter.getNormalizedBands()
        
        // With 64 bands, Band 0 covers roughly 0-344Hz.
        // This is pure Bass/Kick territory, minimizing vocal interference.
        val bass = bands[0] / 255f
        
        // Beat Detection Algorithm:
        // Use a running average of bass energy to detect spikes relative to the song's current "energy"
        // This works better than hard thresholds or single-frame comparisons.
        lastBass = (lastBass * 0.95f) + (bass * 0.05f) // Slower tracking (don't absorb the beat)
        
        // Threshold:
        // 1. Dynamic: Spike over average (lowered to 1.15x for sensitivity)
        // 2. Absolute: If it's just really loud (> 0.6), trigger anyway
        val isDynamicBeat = bass > (lastBass * 1.15f) && bass > 0.1f
        val isLoudBeat = bass > 0.6f
        
        val isBeat = isDynamicBeat || isLoudBeat
        
        val volume = loudnessMeter.getNormalizedLoudness() / 1024f
        
        // Spawn Ripple on BEAT match
        if (isBeat && cooldown <= 0) {
            val ripple = ripples.firstOrNull { !it.active }
            if (ripple != null) {
                ripple.active = true
                ripple.radius = 0f
                ripple.maxRadius = maxDim * 0.6f
                // Pick color from palette based on angle or random
                val hue = (scanAngle / 360f * 255).toInt()
                ripple.color = _palette.getInterpolatedInt(hue)
                cooldown = 15 // Faster cooldown allowed for beats
            }
        }
        if (cooldown > 0) cooldown--

        // 1. Draw Scanner Sweep
        // Constant speed for realism
        scanAngle = (scanAngle + scanSpeed) % 360f
        
        val rad = Math.toRadians(scanAngle.toDouble())
        val scanX = cx + cos(rad).toFloat() * maxDim
        val scanY = cy + sin(rad).toFloat() * maxDim
        
        // Draw Sector (Fading trail) - Optional, maybe just line for contrast?
        // Let's draw a thick line for the scanner arm
        paint.color = Color.GREEN // Classic Sonar color, or maybe white?
        paint.strokeWidth = 5f
        paint.alpha = 255
        canvas.drawLine(cx, cy, scanX, scanY, paint)
        
        // 2. Draw Ripples (The "Pings")
        paint.style = Paint.Style.STROKE
        for (r in ripples) {
            if (!r.active) continue
            
            r.radius += 5f + (volume * 10f) // Expand faster on volume
            
            if (r.radius > r.maxRadius) {
                r.active = false
                continue
            }
            
            val progress = r.radius / r.maxRadius
            val alpha = (1.0f - progress).coerceIn(0f, 1f)
            
            paint.color = r.color
            paint.alpha = (alpha * 255).toInt()
            // Thinner lines since they are expanding (moving)
            paint.strokeWidth = 8f + (volume * 10f) 
            
            canvas.drawCircle(cx, cy, r.radius, paint)
        }
    }

    override fun destroy() {
        fftMeter.stop()
        loudnessMeter.stop()
    }
}
