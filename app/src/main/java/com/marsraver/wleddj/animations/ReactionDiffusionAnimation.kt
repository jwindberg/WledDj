package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import kotlin.random.Random

class ReactionDiffusionAnimation : Animation {

    override fun supportsPrimaryColor(): Boolean = false
    override var primaryColor: Int = Color.WHITE
    override fun supportsSecondaryColor(): Boolean = false
    override var secondaryColor: Int = Color.BLACK
    override fun supportsPalette(): Boolean = true
    override var currentPalette: Palette? = null

    private val WIDTH = 64
    private val HEIGHT = 64
    
    // Two grids for double buffering
    private var grid = Array(WIDTH * HEIGHT) { Chemical(1.0, 0.0) }
    private var next = Array(WIDTH * HEIGHT) { Chemical(1.0, 0.0) }
    
    private class Chemical(var a: Double, var b: Double)
    
    // Gray-Scott Parameters (Coral-like)
    private val dA = 1.0
    private val dB = 0.5
    private val feed = 0.055
    private val k = 0.062
    
    // reusable objects
    private val paint = Paint()
    private val bitmap = android.graphics.Bitmap.createBitmap(WIDTH, HEIGHT, android.graphics.Bitmap.Config.ARGB_8888)
    private val pixels = IntArray(WIDTH * HEIGHT)
    private val matrix = android.graphics.Matrix()
    
    init {
        // Seed some B
        for (i in 0 until 50) {
            val x = Random.nextInt(WIDTH)
            val y = Random.nextInt(HEIGHT)
            val index = y * WIDTH + x
            grid[index].b = 1.0
        }
        
        // Block seed in center
        for (y in HEIGHT/2 - 5 until HEIGHT/2 + 5) {
             for (x in WIDTH/2 - 5 until WIDTH/2 + 5) {
                 grid[y * WIDTH + x].b = 1.0
             }
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Update Simulation
        // Run multiple steps per frame for speed
        for (step in 0 until 4) {
             update()
        }

        // Render
        for (i in grid.indices) {
            val a = grid[i].a
            val b = grid[i].b
            val valDiff = (a - b)
            
            // Map (a-b) to 0..1 range roughly
            // Usually we visualize B concentration or A-B
            
            var v = b // Visualize concentration of B
            v = v.coerceIn(0.0, 1.0)
            
            if (currentPalette != null) {
                pixels[i] = currentPalette!!.getColorAt(v).toInt()
            } else {
                // Fallback Grayscale
                val brightness = (v * 255).toInt()
                pixels[i] = (0xFF shl 24) or (brightness shl 16) or (brightness shl 8) or brightness
            }
        }
        
        bitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        
        // Scale to canvas
        matrix.reset()
        matrix.setScale(width / WIDTH, height / HEIGHT)
        paint.isFilterBitmap = true // Smooth look
        canvas.drawBitmap(bitmap, matrix, paint)
    }

    private fun update() {
        for (x in 0 until WIDTH) {
            for (y in 0 until HEIGHT) {
                val idx = y * WIDTH + x
                
                val a = grid[idx].a
                val b = grid[idx].b
                
                val laplace = laplace(x, y)
                
                val newA = a + (dA * laplace.a - a * b * b + feed * (1 - a))
                val newB = b + (dB * laplace.b + a * b * b - (k + feed) * b)
                
                next[idx].a = newA.coerceIn(0.0, 1.0)
                next[idx].b = newB.coerceIn(0.0, 1.0)
            }
        }
        
        // Swap
        val temp = grid
        grid = next
        next = temp
    }
    
    private fun laplace(x: Int, y: Int): Chemical {
        var sumA = 0.0
        var sumB = 0.0
        
        // Convolution:
        //  0.05  0.2  0.05
        //  0.2  -1.0  0.2
        //  0.05  0.2  0.05
        
        for (dy in -1..1) {
            for (dx in -1..1) {
                 var sx = x + dx
                 var sy = y + dy
                 
                 // Wrap
                 if (sx < 0) sx += WIDTH
                 if (sx >= WIDTH) sx -= WIDTH
                 if (sy < 0) sy += HEIGHT
                 if (sy >= HEIGHT) sy -= HEIGHT
                 
                 val idx = sy * WIDTH + sx
                 val weight = if (dx == 0 && dy == 0) -1.0
                              else if (dx == 0 || dy == 0) 0.2
                              else 0.05
                              
                 sumA += grid[idx].a * weight
                 sumB += grid[idx].b * weight
            }
        }
        return Chemical(sumA, sumB)
    }

    override fun onTouch(touchX: Float, touchY: Float): Boolean {
        // Add B at touch point? 
        // Or Reset?
        // Let's reset but with random seed
        for (i in grid.indices) {
            grid[i].a = 1.0
            grid[i].b = 0.0
        }
        // Seed center
        for (y in HEIGHT/2 - 5 until HEIGHT/2 + 5) {
             for (x in WIDTH/2 - 5 until WIDTH/2 + 5) {
                 grid[y * WIDTH + x].b = 1.0
             }
        }
        return true
    }
}
