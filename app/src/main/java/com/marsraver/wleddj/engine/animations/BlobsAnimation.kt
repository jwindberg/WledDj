package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Blobs Animation - Organic Wobbly Vector Shapes.
 * Replaces simple circles with dynamic spline paths.
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
        
        // Wobble state
        val phaseOffsets = FloatArray(8) { Random.nextFloat() * 6.28f }
        val wobblespeeds = FloatArray(8) { 0.05f + Random.nextFloat() * 0.1f }
    }

    private val blobs = mutableListOf<Blob>()
    private val paint = Paint().apply { isAntiAlias = true }
    private val blobPath = Path()
    
    // Params
    private var paramSpeed: Int = 128
    private var paramIntensity: Int = 128
    private val BLOB_COUNT = 8 // Fewer, but more complex
    
    private var time = 0f

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Init
        if (blobs.isEmpty()) {
            repeat(BLOB_COUNT) {
                blobs.add(spawnBlob(width, height))
            }
        }
        
        // Clear
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        
        // Update & Draw
        val speedMult = 0.5f + (paramSpeed / 255f) * 2.0f
        time += 0.1f * speedMult
        
        blobs.forEach { blob ->
            // Move
            blob.x += blob.vx * speedMult
            blob.y += blob.vy * speedMult
            
            // Bounce
            val bounceMargin = blob.radius * 1.5f
            if (blob.x < bounceMargin) { blob.x = bounceMargin; blob.vx *= -1 }
            if (blob.x > width - bounceMargin) { blob.x = width - bounceMargin; blob.vx *= -1 }
            if (blob.y < bounceMargin) { blob.y = bounceMargin; blob.vy *= -1 }
            if (blob.y > height - bounceMargin) { blob.y = height - bounceMargin; blob.vy *= -1 }
            
            // Draw Blobby Path
            paint.color = blob.color
            paint.alpha = blob.alpha
            paint.style = Paint.Style.FILL
            
            drawWobblyBlob(canvas, blob)
        }
    }
    
    private fun drawWobblyBlob(c: Canvas, b: Blob) {
        blobPath.reset()
        val segments = 8
        val angleStep = (Math.PI * 2 / segments).toFloat()
        
        // Calculate points
        val pointsX = FloatArray(segments)
        val pointsY = FloatArray(segments)
        
        for (i in 0 until segments) {
            // Wobble radius for this vertex
            // r = baseR + sin(time * speed + phase) * variance
            val wobble = sin(time * b.wobblespeeds[i] + b.phaseOffsets[i]) * (b.radius * 0.3f)
            val r = b.radius + wobble
            
            val theta = i * angleStep + (time * 0.05f) // Slowly rotate
            pointsX[i] = b.x + cos(theta) * r
            pointsY[i] = b.y + sin(theta) * r
        }
        
        // Construct smooth path using cubic beziers between midpoints
        // Or simpler: Connect points via quadTo?
        // Let's use the midpoint technique for smoothness.
        
        // Start at midpoint between last and first
        val midX0 = (pointsX[segments-1] + pointsX[0]) / 2f
        val midY0 = (pointsY[segments-1] + pointsY[0]) / 2f
        blobPath.moveTo(midX0, midY0)
        
        for (i in 0 until segments) {
            val nextI = (i + 1) % segments
            val midX = (pointsX[i] + pointsX[nextI]) / 2f
            val midY = (pointsY[i] + pointsY[nextI]) / 2f
            
            // Curve from previous midpoint to this midpoint, using the vertex (pointsX[i]) as control
            blobPath.quadTo(pointsX[i], pointsY[i], midX, midY)
        }
        blobPath.close() // Should be closed anyway
        
        c.drawPath(blobPath, paint)
    }
    
    private fun spawnBlob(w: Float, h: Float): Blob {
        val b = Blob()
        b.radius = (w + h) / 15f * (0.5f + Random.nextFloat()) // Larger blobs
        b.x = Random.nextFloat() * (w - 2*b.radius) + b.radius
        b.y = Random.nextFloat() * (h - 2*b.radius) + b.radius
        
        val angle = Random.nextFloat() * 6.28f
        val speed = 1f + Random.nextFloat() * 2f
        b.vx = cos(angle) * speed
        b.vy = sin(angle) * speed
        
        // Color
        val colorIndex = Random.nextInt(256)
        b.color = _palette.getInterpolatedInt(colorIndex)
        b.alpha = 150 + Random.nextInt(105) // Semi-transparent
        
        return b
    }
}
