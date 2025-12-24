package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.BeatDetector
import com.marsraver.wleddj.engine.audio.FftMeter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Spectrum Tree Animation
 * A sound-reactive Christmas tree.
 * - Ornaments light up based on FFT bands.
 * - Star pulses to the beat.
 * - Snow falls in background.
 */
class SpectrumTreeAnimation : Animation {

    private val fftMeter = FftMeter(16) // 16 bands for ornaments
    
    // Paints
    private val treePaint = Paint().apply { 
        color = Color.rgb(10, 80, 20) // Dark Green
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val ornamentPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val starPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val snowPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 180
    }

    // Snow System
    private data class Snowflake(var x: Float, var y: Float, var speed: Float, var size: Float)
    private val snow = mutableListOf<Snowflake>()
    
    init {
        // Pre-populate snow
        repeat(50) {
            snow.add(Snowflake(
                Random.nextFloat(), // Normalized X
                Random.nextFloat(), // Normalized Y
                Random.nextFloat() * 0.01f + 0.005f, 
                Random.nextFloat() * 3f + 2f
            ))
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // 1. Draw Background Snow
        updateSnow(width, height)
        drawSnow(canvas, width, height)

        // 2. Draw Tree
        // Centered, taking up 80% height
        val treeBaseY = height * 0.9f
        val treeTopY = height * 0.1f
        val treeWidth = width * 0.6f
        val centerX = width / 2f
        
        val path = Path()
        path.moveTo(centerX, treeTopY) // Top
        path.lineTo(centerX + treeWidth/2, treeBaseY) // Right
        path.lineTo(centerX - treeWidth/2, treeBaseY) // Left
        path.close()
        
        canvas.drawPath(path, treePaint)
        
        // 3. Draw Ornaments (FFT)
        // We'll place ornaments in a Zig-Zag or random pattern down the tree?
        // Or distinct "Rows".
        // Let's do 4 rows of ornaments.
        // Row 0 (Top): 1 band (High)
        // Row 1: 2 bands
        // Row 2: 3 bands
        // Row 3 (Bottom): 4 bands
        // Total 10 bands used (out of 16).
        
        // Or simpler: Projected positions.
        val bands = fftMeter.getNormalizedBands() // 0-255
        
        val layers = 5
        var bandIndex = 0
        
        for (i in 1..layers) {
            val ySync = i / (layers + 1f) // 0.16, 0.33, etc.
            val yPos = treeTopY + (treeBaseY - treeTopY) * ySync
            val rowWidth = (treeWidth * ySync) * 0.8f // Width at this Y
            
            // Number of ornaments in this row increases with depth
            val count = i + 1 
            val spacing = rowWidth / (count - 1).coerceAtLeast(1)
            val startX = centerX - rowWidth / 2f
            
            for (j in 0 until count) {
                if (bandIndex >= bands.size) break
                
                val level = bands[bandIndex] / 255f
                val size = 5f + (level * 15f) // Pulse size
                val brightness = (level * 255).toInt()
                
                // Color map: Low freq (start of bands) = Blue/Purple?
                // Christmas: Red/Gold/Silver/Blue
                val color = when(bandIndex % 4) {
                    0 -> Color.rgb(255, 0, 0) // Red
                    1 -> Color.rgb(255, 215, 0) // Gold
                    2 -> Color.rgb(0, 0, 255) // Blue
                    3 -> Color.rgb(192, 192, 192) // Silver
                    else -> Color.WHITE
                }
                
                // Dim non-active ones
                var r = Color.red(color); var g = Color.green(color); var b = Color.blue(color)
                if (level < 0.2f) {
                    r /= 4; g /= 4; b /= 4
                } else {
                    // Boost active
                    r = (r + brightness).coerceAtMost(255)
                    g = (g + brightness).coerceAtMost(255)
                    b = (b + brightness).coerceAtMost(255)
                }
                
                ornamentPaint.color = Color.rgb(r, g, b)
                
                val ox = startX + j * spacing
                canvas.drawCircle(ox, yPos, size, ornamentPaint)
                
                bandIndex++
            }
        }
        
        // 4. Draw Star (Beat)
        val beatLevel = BeatDetector.getLevel()
        val starSize = 20f + (beatLevel * 20f)
        val starColor = if (beatLevel > 0.5f) Color.WHITE else Color.YELLOW
        starPaint.color = starColor
        
        drawStar(canvas, centerX, treeTopY, starSize)
    }
    
    private fun updateSnow(w: Float, h: Float) {
        val wind = (sin(System.currentTimeMillis() / 1000.0) * 0.002f).toFloat()
        // Boost speed with volume
        val energy = BeatDetector.getLevel() * 0.02f
        
        snow.forEach { flake ->
            flake.y += flake.speed + energy
            flake.x += wind
            
            if (flake.y > 1.0f) flake.y = -0.1f
            if (flake.x > 1.0f) flake.x = 0f
            if (flake.x < 0f) flake.x = 1f
        }
    }
    
    private fun drawSnow(canvas: Canvas, w: Float, h: Float) {
        snow.forEach { flake ->
            canvas.drawCircle(flake.x * w, flake.y * h, flake.size, snowPaint)
        }
    }
    
    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        // Pentagram
        val path = Path()
        val outerR = size
        val innerR = size * 0.4f
        var rot = -Math.PI / 2 // Point Up
        
        path.moveTo((cx + cos(rot) * outerR).toFloat(), (cy + sin(rot) * outerR).toFloat())
        
        val step = Math.PI / 5
        for (i in 1 until 5) {
             rot += step
             path.lineTo((cx + cos(rot) * innerR).toFloat(), (cy + sin(rot) * innerR).toFloat())
             rot += step
             path.lineTo((cx + cos(rot) * outerR).toFloat(), (cy + sin(rot) * outerR).toFloat())
        }
        path.close()
        canvas.drawPath(path, starPaint)
    }

    override fun destroy() {
        fftMeter.stop()
    }
}
