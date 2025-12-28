package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.*

/**
 * Spinning Globe Animation
 * Simulates a rotating 3D earth using ray-casting/spherical projection and Perlin noise for terrain.
 * Ported from WledFx/SpinningGlobeAnimation.kt
 */
class GlobeAnimation : Animation {

    private val paint = Paint().apply { isAntiAlias = false; isDither = false }
    
    // Rotation state
    private var rotation: Double = 0.0
    private var cloudRotation: Double = 0.0
    private var paramSpeed = 128

    // Constants
    private val radiusPercentage = 0.85 // Globe size relative to min dimension

    // Colors (ARGB Ints)
    private val deepOcean = Color.rgb(0, 5, 30)
    private val shallowOcean = Color.rgb(0, 40, 100)
    private val landLow = Color.rgb(30, 100, 20)
    private val landHigh = Color.rgb(100, 80, 40)
    private val cloudColor = Color.rgb(255, 255, 255)
    private val atmosphereColor = Color.rgb(100, 150, 255)
    private val starColor = Color.rgb(100, 100, 100)

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Optimization: Render to a fixed low-resolution buffer (64x64)
        // 64x64 = 4096 pixels. Very fast, and matches LED Matrix look.
        val renderW = 64
        val renderH = 64
        
        // Init buffers once
        if (cachedBitmap == null || cachedBitmap?.width != renderW || cachedBitmap?.height != renderH) {
            cachedBitmap = android.graphics.Bitmap.createBitmap(renderW, renderH, android.graphics.Bitmap.Config.ARGB_8888)
            pixelBuffer = IntArray(renderW * renderH)
        }
        val bmp = cachedBitmap!!
        val pixels = pixelBuffer!!
        
        // Update rotation
        val speedFactor = paramSpeed / 128.0
        rotation += 0.02 * speedFactor
        cloudRotation += 0.025 * speedFactor 

        val cx = renderW / 2.0
        val cy = renderH / 2.0
        val minDim = min(renderW, renderH)
        val globeRadius = (minDim / 2.0) * radiusPercentage
        val globeRadiusSq = globeRadius * globeRadius
        
        for (y in 0 until renderH) {
            val dy = y - cy
            val dySq = dy * dy
            val rowOffset = y * renderW
            
            for (x in 0 until renderW) {
                val dx = x - cx
                val distSq = dx*dx + dySq
                
                if (distSq > globeRadiusSq) {
                    pixels[rowOffset + x] = Color.BLACK
                    continue
                }
                
                val z = sqrt(globeRadiusSq - distSq)

                // Normal vector
                val nx = dx / globeRadius
                val ny = dy / globeRadius
                val nz = z / globeRadius

                // 3D Point on surface
                val earthX = nx * cos(rotation) - nz * sin(rotation)
                val earthZ = nx * sin(rotation) + nz * cos(rotation)
                val earthY = ny

                // Sample Terrain Noise
                val noiseScale = 2.0
                val n = MathUtils.perlinNoise(earthX * noiseScale + 100.0, earthY * noiseScale, earthZ * noiseScale) 

                var color = if (n < 0.05) {
                     if (n < -0.3) deepOcean else shallowOcean
                } else {
                     if (n < 0.4) landLow else landHigh
                }

                // Ice Caps
                if (abs(ny) > 0.9) {
                    val iceFactor = ((abs(ny) - 0.9) * 10.0).coerceIn(0.0, 1.0)
                    color = blend(color, Color.WHITE, iceFactor)
                }

                // Clouds
                val cloudX = nx * cos(cloudRotation) - nz * sin(cloudRotation)
                val cloudZ = nx * sin(cloudRotation) + nz * cos(cloudRotation)
                val cloudNoise = MathUtils.perlinNoise(cloudX * 2.5 + 500.0, earthY * 2.5, cloudZ * 2.5)

                if (cloudNoise > 0.2) {
                    val intensity = ((cloudNoise - 0.2) * 2.0).coerceIn(0.0, 1.0)
                    color = blend(color, cloudColor, intensity * 0.7)
                }

                // Lighting
                val dot = (nx * -0.4 + ny * -0.4 + nz * 0.82)
                val lightIntensity = (dot * 0.8 + 0.2).coerceIn(0.1, 1.0)
                
                color = scaleBrightness(color, lightIntensity)

                // Specular
                if (n < 0.05 && dot > 0.95) {
                    val spec = ((dot - 0.95) * 20.0).coerceIn(0.0, 1.0)
                    color = blend(color, Color.WHITE, spec * 0.6)
                }

                // Atmosphere
                val fresnel = (1.0 - nz).pow(4.0)
                if (fresnel > 0.0) {
                    val atmAlpha = (fresnel * 0.8).coerceIn(0.0, 1.0)
                    color = blend(color, atmosphereColor, atmAlpha)
                }

                pixels[rowOffset + x] = color
            }
        }
        
        bmp.setPixels(pixels, 0, renderW, 0, 0, renderW, renderH)
        
        // Draw Scaled to Canvas
        // Use rects to draw exactly filling the view
        val destRect = android.graphics.RectF(0f, 0f, width, height)
        paint.isFilterBitmap = false // Nearest Neighbor for retro LED look
        canvas.drawBitmap(bmp, null, destRect, paint)
    }
    
    private var cachedBitmap: android.graphics.Bitmap? = null
    private var pixelBuffer: IntArray? = null
    
    // Helper: Blend colors
    // factor 0..1
    private fun blend(c1: Int, c2: Int, factor: Double): Int {
        val f = factor.toFloat()
        val c1A = Color.alpha(c1); val c1R = Color.red(c1); val c1G = Color.green(c1); val c1B = Color.blue(c1)
        val c2A = Color.alpha(c2); val c2R = Color.red(c2); val c2G = Color.green(c2); val c2B = Color.blue(c2)
        
        val r = c1R + (c2R - c1R) * f
        val g = c1G + (c2G - c1G) * f
        val b = c1B + (c2B - c1B) * f
        
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }
    
    // Helper: Scale Brightness
    private fun scaleBrightness(color: Int, intensity: Double): Int {
        val r = (Color.red(color) * intensity).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * intensity).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * intensity).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    override var currentPalette: Palette? = null
    override fun supportsPalette() = false
}
