package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import kotlin.random.Random

/**
 * Blobs Animation - Vector Bubbles.
 * Smooth floating circles with alpha blending.
 */
class BlobsAnimation : Animation {

    private var _palette: Palette = Palettes.get("Rainbow") ?: Palettes.getDefault()
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // State
    private class Blob {
        var x: Float = 0f
        var y: Float = 0f
        var vx: Float = 0f
        var vy: Float = 0f
        var radius: Float = 10f
        var color: Int = Color.WHITE
        var alpha: Int = 200
    }

    private val blobs = mutableListOf<Blob>()
    private val paint = Paint().apply { isAntiAlias = true }
    
    // Params
    private var paramSpeed: Int = 128
    private var paramIntensity: Int = 128
    private val BLOB_COUNT = 10

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Init
        if (blobs.isEmpty()) {
            repeat(BLOB_COUNT) {
                blobs.add(spawnBlob(width, height))
            }
        }
        
        // Clear
        canvas.drawColor(Color.BLACK)
        
        // Update & Draw
        val speedMult = 0.5f + (paramSpeed / 255f) * 2.0f
        
        // Use additive-like blending visually by drawing with alpha?
        // Standard alpha blending is fine for "bubbles".
        
        blobs.forEach { blob ->
            // Move
            blob.x += blob.vx * speedMult
            blob.y += blob.vy * speedMult
            
            // Bounce
            if (blob.x < blob.radius) {
                blob.x = blob.radius
                blob.vx = -blob.vx
            } else if (blob.x > width - blob.radius) {
                blob.x = width - blob.radius
                blob.vx = -blob.vx
            }
            
            if (blob.y < blob.radius) {
                blob.y = blob.radius
                blob.vy = -blob.vy
            } else if (blob.y > height - blob.radius) {
                blob.y = height - blob.radius
                blob.vy = -blob.vy
            }
            
            // Draw
            paint.color = blob.color
            paint.alpha = blob.alpha // Alpha is part of color in int, but Paint.alpha overrides?
            // Safer:
            // paint.color = (blob.color and 0x00FFFFFF) or (blob.alpha shl 24)
            // But let's stick to simple setup.
            
            canvas.drawCircle(blob.x, blob.y, blob.radius, paint)
        }
    }
    
    private fun spawnBlob(w: Float, h: Float): Blob {
        val b = Blob()
        b.radius = (w + h) / 20f * (0.5f + Random.nextFloat()) // Random size
        b.x = Random.nextFloat() * (w - 2*b.radius) + b.radius
        b.y = Random.nextFloat() * (h - 2*b.radius) + b.radius
        
        val angle = Random.nextFloat() * 6.28f
        val speed = 2f + Random.nextFloat() * 3f
        b.vx = kotlin.math.cos(angle) * speed
        b.vy = kotlin.math.sin(angle) * speed
        
        // Color
        val colorIndex = Random.nextInt(256)
        b.color = _palette.getInterpolatedInt(colorIndex)
        b.alpha = 100 + Random.nextInt(100) // Semi-transparent
        
        return b
    }
}
