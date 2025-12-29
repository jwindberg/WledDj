package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.sqrt

class FractalZoomAnimation : Animation {

    override fun supportsPrimaryColor(): Boolean = false
    override var primaryColor: Int = Color.BLACK
    override fun supportsSecondaryColor(): Boolean = false
    override var secondaryColor: Int = Color.BLACK
    override fun supportsPalette(): Boolean = true
    override var currentPalette: Palette? = null

    private val paint = Paint()
    
    // Interesting point in Mandelbrot set
    private var targetX = -0.743643887037158704752191506114774
    private var targetY = 0.131825904205311970493132056385139
    private val DEFAULT_X = -0.743643887037158704752191506114774
    private val DEFAULT_Y = 0.131825904205311970493132056385139
    
    private var zoom = 1.0
    private val zoomSpeed = 1.02 // Slower zoom for better tracking
    private val maxZoom = 1000000000000.0 

    private val RENDER_WIDTH = 128
    private val RENDER_HEIGHT = 128
    private val pixels = IntArray(RENDER_WIDTH * RENDER_HEIGHT)
    private val bitmap = android.graphics.Bitmap.createBitmap(RENDER_WIDTH, RENDER_HEIGHT, android.graphics.Bitmap.Config.ARGB_8888)
    private val matrix = android.graphics.Matrix()

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Scale coordinate system to aspect ratio
        val aspectRatio = width / height
        val curW = 3.0 / zoom
        val curH = curW / aspectRatio
        
        val startX = targetX - curW / 2.0
        val startY = targetY - curH / 2.0

        // Adjust max iterations based on zoom depth for detail
        val maxIterations = (50 + log2(zoom) * 10).toInt()

        for (py in 0 until RENDER_HEIGHT) {
            val y0 = startY + (py.toDouble() / RENDER_HEIGHT) * curH
            val rowOffset = py * RENDER_WIDTH
            for (px in 0 until RENDER_WIDTH) {
                val x0 = startX + (px.toDouble() / RENDER_WIDTH) * curW
                
                var x = 0.0
                var y = 0.0
                var iteration = 0
                
                // Optimized loop
                val limit = 4.0
                while (x*x + y*y <= limit && iteration < maxIterations) {
                    val xtemp = x*x - y*y + x0
                    y = 2*x*y + y0
                    x = xtemp
                    iteration++
                }
                
                val color = if (iteration < maxIterations) {
                    // Smooth coloring
                    val logZn = ln(x*x + y*y) / 2.0
                    val nu = ln(logZn / ln(2.0)) / ln(2.0)
                    val smoothIter = iteration + 1.0 - nu
                    
                    // Map to color hue
                    val colorHue = (smoothIter * 5.0).toFloat() % 360f
                    Color.HSVToColor(floatArrayOf(colorHue, 1f, 1f))
                } else {
                    Color.BLACK
                }
                pixels[rowOffset + px] = color
            }
        }
        
        // Auto-pan logic: Find "Center of Interest"
        // Interest = edge presence (pixel difference)
        var sumX = 0.0
        var sumY = 0.0
        var totalInterest = 0.0
        
        for (py in 0 until RENDER_HEIGHT - 1) {
            val rowOffset = py * RENDER_WIDTH
            for (px in 0 until RENDER_WIDTH - 1) {
                val pOriginal = pixels[rowOffset + px]
                val pRight = pixels[rowOffset + px + 1]
                val pDown = pixels[rowOffset + RENDER_WIDTH + px]
                
                // Ignore solid black areas (inside set)
                // We want the boundary between Black and Color.
                // Or high contrast color-color edges.
                
                var interest = 0.0
                
                // If this pixel is black, it's boring (unless it's an edge)
                // If this pixel is colored, checking neighbors
                
                val pIsBlack = (pOriginal == Color.BLACK)
                val rIsBlack = (pRight == Color.BLACK)
                val dIsBlack = (pDown == Color.BLACK)
                
                if (pIsBlack) {
                    // Start of boundary?
                    if (!rIsBlack || !dIsBlack) {
                         // This is a Black->Color edge. HIGH interest.
                         interest = 10.0
                    }
                } else {
                    // This is colored.
                    if (rIsBlack || dIsBlack) {
                        // Color->Black edge. HIGH interest.
                        interest = 10.0
                    } 
                    // REMOVED: Color-Color interest. This distracts from the coastline.
                }
                
                if (interest > 0) {
                    sumX += px * interest
                    sumY += py * interest
                    totalInterest += interest
                }
            }
        }
        
        if (totalInterest > 10) { // Avoid divide by zero or extreme noise
             val centerX = sumX / totalInterest
             val centerY = sumY / totalInterest
             
             // Deviation from center (0..RENDER_WIDTH)
             val devX = (centerX - RENDER_WIDTH / 2.0)
             val devY = (centerY - RENDER_HEIGHT / 2.0)
             
             // Move target in world space
             // Scale factors
             val pixelW = curW / RENDER_WIDTH
             val pixelH = curH / RENDER_HEIGHT
             
             // Aggressively steer towards interest (0.15 factor, up from 0.05)
             targetX += devX * pixelW * 0.15
             targetY += devY * pixelH * 0.15
        }
        
        bitmap.setPixels(pixels, 0, RENDER_WIDTH, 0, 0, RENDER_WIDTH, RENDER_HEIGHT)
        
        // Scale to fit canvas
        matrix.reset()
        matrix.setScale(width / RENDER_WIDTH, height / RENDER_HEIGHT)
        canvas.drawBitmap(bitmap, matrix, paint)
        
        // Update zoom
        zoom *= zoomSpeed
        if (zoom > maxZoom) {
            zoom = 1.0
            // Reset to interesting start
            targetX = DEFAULT_X
            targetY = DEFAULT_Y
        }
    }

    override fun onTouch(touchX: Float, touchY: Float): Boolean {
        // Reset or pan?
        zoom = 1.0
        targetX = DEFAULT_X
        targetY = DEFAULT_Y
        return true
    }
}
