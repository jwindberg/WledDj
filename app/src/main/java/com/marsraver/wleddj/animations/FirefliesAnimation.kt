package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.FftMeter
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import kotlin.math.*
import kotlin.random.Random

/**
 * Fireflies animation - Magical fireflies that pulse and move to music.
 * Ported from WledFx to Android Canvas.
 */
class FirefliesAnimation : Animation {

    private data class Firefly(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var targetX: Float,
        var targetY: Float,
        var brightness: Float,
        var pulsePhase: Double,
        var frequencyBand: Int,
        var color: Int
    )

    private val fireflies = mutableListOf<Firefly>()
    private val paint = Paint().apply { 
        isAntiAlias = true 
        style = Paint.Style.FILL
    }
    
    // Audio
    private val fftMeter = FftMeter(bands = 32)
    private val loudnessMeter = LoudnessMeter()
    
    private var isInitialized = false
    private val random = Random.Default
    
    // Base class params used here: paramSpeed
    private var paramSpeed: Int = 128
    
    // Palette Support
    private var _palette: com.marsraver.wleddj.engine.color.Palette = com.marsraver.wleddj.engine.color.Palette.FOREST
    override var currentPalette: com.marsraver.wleddj.engine.color.Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true
    
    // Speed Support
    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        paramSpeed = (speed * 255).toInt().coerceIn(1, 255)
    }
    override fun getSpeed(): Float = paramSpeed / 255f
    
    // Dark Night Background
    private val nightColor = Color.BLACK // Pure Black

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        if (!isInitialized) {
            initFireflies(width, height)
            isInitialized = true
        }
        
        // 1. Draw Background (Very dark version of primary or black)
        canvas.drawColor(nightColor)
        
        // 2. Audio Data
        val bands = fftMeter.getNormalizedBands() 
        val loudness = loudnessMeter.getCurrentLoudness() 
        val globalBrightness = (loudness / 1024.0 * 0.7 + 0.3).coerceIn(0.3, 1.0)
        
        fireflies.forEach { firefly ->
            updateFirefly(firefly, width, height, bands)
            
            // Pulse logic
            val pulse = (sin(firefly.pulsePhase) * 0.3 + 0.7).coerceIn(0.0, 1.0).toFloat()
            val finalBrightness = (firefly.brightness * pulse * globalBrightness).toFloat()
            
            // Allow palette to determine color based on ID/Band
            // Map firefly ID or band to palette index (0-255)
            // Use firefly.frequencyBand to pick color from palette to match audio band
            val paletteIndex = (firefly.frequencyBand * 255 / 32)
            firefly.color = _palette.getInterpolatedInt(paletteIndex)
            
            val alpha = (finalBrightness * 255).toInt().coerceIn(0, 255)
            
            // Draw Glow (Outer)
            paint.color = firefly.color
            paint.alpha = (alpha * 0.3f).toInt()
            val glowRadius = 6f // Was 12f
            canvas.drawCircle(firefly.x, firefly.y, glowRadius, paint)
            
            // Draw Core (Inner)
            paint.alpha = alpha
            val coreRadius = 1.5f // Was 3f
            canvas.drawCircle(firefly.x, firefly.y, coreRadius, paint)
        }
    }
    
    private fun initFireflies(w: Float, h: Float) {
        fireflies.clear()
        val numFireflies = 25
        
        for (i in 0 until numFireflies) {
            fireflies.add(
                Firefly(
                    x = random.nextFloat() * w,
                    y = random.nextFloat() * h,
                    vx = 0f,
                    vy = 0f,
                    targetX = random.nextFloat() * w,
                    targetY = random.nextFloat() * h,
                    brightness = 0.5f,
                    pulsePhase = random.nextDouble() * 2 * PI,
                    frequencyBand = i % 32,
                    color = Color.WHITE // Will be updated by palette in draw
                )
            )
        }
    }

    private fun updateFirefly(firefly: Firefly, w: Float, h: Float, bands: IntArray) {
        // Audio Reactivity
        val bandValue = if (firefly.frequencyBand < bands.size) {
            bands[firefly.frequencyBand]
        } else { 0 }
        
        val speedFactor = paramSpeed / 128.0
        
        // Brightness Target
        val targetBrightness = (bandValue / 255.0 * 0.8 + 0.2).toFloat()
        firefly.brightness += (targetBrightness - firefly.brightness) * 0.1f
        
        // Pulse Phase Speed
        firefly.pulsePhase += (0.05 + (bandValue / 255.0 * 0.1)) * speedFactor
        
        // Movement Logic
        val dx = firefly.targetX - firefly.x
        val dy = firefly.targetY - firefly.y
        val distance = sqrt(dx * dx + dy * dy)
        
        // Re-target if close
        if (distance < 10.0f) {
            firefly.targetX = random.nextFloat() * w
            firefly.targetY = random.nextFloat() * h
        }
        
        // Acceleration
        val acceleration = 0.05f * speedFactor.toFloat()
        val maxSpeed = ((0.5 + (bandValue / 255.0 * 2.0)) * speedFactor).toFloat() // More speed on beat
        
        if (distance > 0) {
            val ax = (dx / distance) * acceleration
            val ay = (dy / distance) * acceleration
            firefly.vx += ax
            firefly.vy += ay
        }
        
        // Limit Speed
        val speed = sqrt(firefly.vx * firefly.vx + firefly.vy * firefly.vy)
        if (speed > maxSpeed) {
            firefly.vx = (firefly.vx / speed) * maxSpeed
            firefly.vy = (firefly.vy / speed) * maxSpeed
        }
        
        // Apply Velocity
        firefly.x += firefly.vx
        firefly.y += firefly.vy
        
        // Wrap Around
        if (firefly.x < 0) firefly.x += w
        if (firefly.x >= w) firefly.x -= w
        if (firefly.y < 0) firefly.y += h
        if (firefly.y >= h) firefly.y -= h
    }

    override fun destroy() {
        fftMeter.stop()
        loudnessMeter.stop()
    }
}
