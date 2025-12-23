package com.marsraver.wleddj.engine.animations

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
    
    // Palette
    private val fireflyColors = listOf(
        Color.rgb(255, 220, 100), // Yellow-Gold
        Color.rgb(200, 255, 100), // Lime-Green
        Color.rgb(255, 200, 80),  // Orange-Gold
        Color.rgb(180, 255, 120), // Pale Green
        Color.rgb(255, 230, 120)  // Light Yellow
    )
    
    // Dark Night Background
    private val nightColor = Color.rgb(0, 0, 15) // Deep Blue/Black

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        if (!isInitialized) {
            initFireflies(width, height)
            isInitialized = true
        }
        
        // 1. Draw Background
        canvas.drawColor(nightColor)
        
        // 2. Audio Data
        val bands = fftMeter.getNormalizedBands() // IntArray(32), 0-255
        val loudness = loudnessMeter.getCurrentLoudness() 
        val globalBrightness = (loudness / 1024.0 * 0.7 + 0.3).coerceIn(0.3, 1.0)
        
        // 3. Update & Draw Fireflies
        // Use a "Glow" effect. 
        // Simple efficient glow: Draw circle with semi-transparent alpha, then smaller solid circle? 
        // Or RadialGradient? RadialGradient is expensive to create every frame.
        // Let's use simple alpha layers.
        
        fireflies.forEach { firefly ->
            updateFirefly(firefly, width, height, bands)
            
            // Pulse logic
            val pulse = (sin(firefly.pulsePhase) * 0.3 + 0.7).coerceIn(0.0, 1.0).toFloat()
            val finalBrightness = (firefly.brightness * pulse * globalBrightness).toFloat()
            val alpha = (finalBrightness * 255).toInt().coerceIn(0, 255)
            
            // Draw Glow (Outer)
            paint.color = firefly.color
            paint.alpha = (alpha * 0.3f).toInt()
            val glowRadius = 25f 
            canvas.drawCircle(firefly.x, firefly.y, glowRadius, paint)
            
            // Draw Core (Inner)
            paint.alpha = alpha
            val coreRadius = 6f
            canvas.drawCircle(firefly.x, firefly.y, coreRadius, paint)
        }
    }
    
    private fun initFireflies(w: Float, h: Float) {
        fireflies.clear()
        // Density derived from logic: min(16, max(8, count))
        // Let's pick a reasonable number for mobile screen
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
                    color = fireflyColors[i % fireflyColors.size]
                )
            )
        }
    }

    private fun updateFirefly(firefly: Firefly, w: Float, h: Float, bands: IntArray) {
        // Audio Reactivity
        val bandValue = if (firefly.frequencyBand < bands.size) {
            bands[firefly.frequencyBand]
        } else { 0 }
        
        val speedFactor = 1.0 // hardcoded paramSpeed/128
        
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
