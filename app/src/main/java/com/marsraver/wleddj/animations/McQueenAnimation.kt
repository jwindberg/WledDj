package com.marsraver.wleddj.animations

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.marsraver.wleddj.R
import com.marsraver.wleddj.engine.Animation

class McQueenAnimation(context: Context) : Animation {

    private val originalBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.raw.mcqueen_right)
    private var scaledBitmap: Bitmap? = null
    private var lastWidth = -1f
    private var lastHeight = -1f

    private var x = -1f
    private var y = -1f
    
    // Default facing right (matches image)
    private var facingRight = true
    
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    private val matrix = Matrix()

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Initialize position to center if first run
        if (x < 0) {
            x = width / 2f
            y = height / 2f
        }
        
        // Scale bitmap to reasonable size (e.g., 20% of width)
        if (width != lastWidth || height != lastHeight) {
            val targetW = width * 0.25f
            val scale = targetW / originalBitmap.width
            val targetH = originalBitmap.height * scale
            
            scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetW.toInt(), targetH.toInt(), true)
            lastWidth = width
            lastHeight = height
        }
        
        val bmp = scaledBitmap ?: return
        
        // Prepare Matrix for drawing
        matrix.reset()
        
        // Translate to position (centered)
        // We handle flipping by scaling -1 on X axis if needed
        val cx = bmp.width / 2f
        val cy = bmp.height / 2f
        
        if (!facingRight) {
             // Flip horizontally around center of bitmap
             matrix.postScale(-1f, 1f, cx, cy)
        }
        
        matrix.postTranslate(x - cx, y - cy)
        
        canvas.drawBitmap(bmp, matrix, paint)
    }

    override fun onTouch(x: Float, y: Float): Boolean {
        // Determine direction
        // Tolerance to prevent jitter
        if (kotlin.math.abs(x - this.x) > 2f) {
            if (x < this.x) {
                facingRight = false // Moving Left
            } else if (x > this.x) {
                facingRight = true  // Moving Right
            }
        }
        
        this.x = x
        this.y = y
        return true
    }
}
