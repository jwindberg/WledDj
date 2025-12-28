package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.abs
import kotlin.math.sin

/**
 * Polar Lights Animation (Canvas)
 * Smooth, high-res aurora curtains using floating-point Perlin noise.
 */
class PolarLightsAnimation : Animation {

    private var _palette: Palette = Palettes.get("Forest") ?: Palettes.getDefault()
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // Params
    private var paramSpeed: Int = 128
    private var paramIntensity: Int = 128

    // State
    private var timeZ = 0.0
    private val path = Path()
    private val paint = Paint().apply { 
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        canvas.drawColor(Color.BLACK)
        
        // Speed: 0.001 to 0.02
        val speed = 0.002 + (paramSpeed / 255.0) * 0.02
        timeZ += speed
        
        // Intensity determines "Height" or "Activity"
        // Amplitude: How far down they hang
        val amp = height * (0.4f + (paramIntensity/255f) * 0.5f)
        
        // Draw 3 layers of curtains
        drawCurtain(canvas, width, height, timeZ, amp, 0)
        drawCurtain(canvas, width, height, timeZ + 100.0, amp * 0.9f, 1)
        drawCurtain(canvas, width, height, timeZ + 200.0, amp * 0.7f, 2)
    }
    
    private fun drawCurtain(canvas: Canvas, w: Float, h: Float, t: Double, amplitude: Float, layerIdx: Int) {
        path.reset()
        
        // Anchor: Top Left
        path.moveTo(0f, 0f)
        
        val step = 10f
        var x = 0f
        var maxY = 0f
        
        while (x <= w + step) {
            // Perlin Noise
            val scaleX = 0.003
            val n = MathUtils.perlinNoise(x * scaleX, t, layerIdx.toDouble() * 10.0)
            
            // Map -1..1 to 0..1
            val yn = (n + 1.0) / 2.0
            
            // Y is distance DOWN from top (0)
            var y = (yn * amplitude).toFloat()
            
            // Detail
            val n2 = MathUtils.perlinNoise(x * 0.02, t * 2.0, layerIdx.toDouble())
            y += (n2 * amplitude * 0.15).toFloat()
            
            if (y > maxY) maxY = y
            
            path.lineTo(x, y)
            x += step
        }
        
        // Close at Top Right
        path.lineTo(w, 0f)
        path.close()
        
        // Color
        val colorParam = ((t * 0.1) + layerIdx * 0.3) % 1.0
        val baseColor = _palette.getInterpolatedInt((colorParam * 255).toInt())
        
        // Gradient: 
        // Top (0) -> Transparent/Dim
        // Bottom (maxY) -> Bright Color
        // This looks like the "bottom edge" of the aurora glowing
        
        val shader = LinearGradient(0f, 0f, 0f, maxY, 
            Color.TRANSPARENT,
            baseColor,
            Shader.TileMode.CLAMP)
        
        paint.shader = shader
        paint.alpha = 180 
        
        canvas.drawPath(path, paint)
    }
}
