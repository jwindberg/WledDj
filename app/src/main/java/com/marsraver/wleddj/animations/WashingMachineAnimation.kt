package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Washing Machine Animation - Spin Cycle.
 * Rotating vector particles with agitation physics.
 */
class WashingMachineAnimation : Animation {

    private var _palette: Palette = Palette.RAINBOW
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // State
    private var buffer: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    private val paint = Paint().apply { isAntiAlias = true }
    private val fadePaint = Paint().apply { 
        color = Color.BLACK 
        alpha = 40 // Medium trails
    }
    private val clearRect = Rect()

    // Physics
    private var rotation: Float = 0f
    private var rotationSpeed: Float = 0f
    private var timeSeconds: Double = 0.0

    // Particles (Clothes?)
    private data class Item(
        var angleOffset: Float,
        var distance: Float,
        var color: Int,
        var size: Float
    )
    private val items = mutableListOf<Item>()

    // Params
    private var paramSpeed: Int = 128
    
    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val w = width.toInt().coerceAtLeast(1)
        val h = height.toInt().coerceAtLeast(1)
        
        // Init Items
        if (items.isEmpty()) {
            repeat(12) {
                val dist = 0.3f + Random.nextFloat() * 0.4f // 30-70% of radius
                items.add(Item(
                    angleOffset = Random.nextFloat() * 360f,
                    distance = dist,
                    color = Color.WHITE, // Will be set from palette
                    size = 5f + Random.nextFloat() * 10f
                ))
            }
        }

        // 1. Buffer
        if (buffer == null || buffer?.width != w || buffer?.height != h) {
            buffer?.recycle()
            buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(buffer!!)
            bufferCanvas?.drawColor(Color.BLACK)
        }
        val bufCanvas = bufferCanvas ?: return
        
        // 2. Fade
        clearRect.set(0, 0, w, h)
        bufCanvas.drawRect(clearRect, fadePaint)
        
        // 3. Physics (Agitate Cycle)
        timeSeconds += 0.01 + (paramSpeed / 255.0) * 0.02
        
        // Cycle: Spin Fast -> Slow -> Reverse -> Slow
        val cycle = (timeSeconds * 0.5) % 4.0 // 4 phases
        
        val targetSpeed = when {
            cycle < 1.0 -> 10.0f // Forward Spin
            cycle < 1.5 -> 0.5f  // Pause/Slow
            cycle < 2.5 -> -10.0f // Reverse Spin
            else -> -0.5f        // Pause
        }
        
        // Smooth inertia
        rotationSpeed += (targetSpeed - rotationSpeed) * 0.05f
        rotation += rotationSpeed
        
        // 4. Draw
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(width, height) / 2f
        
        items.forEachIndexed { index, item ->
            // Color cycle
            val colIndex = (index * 20 + timeSeconds * 50).toInt() % 256
            item.color = _palette.getInterpolatedInt(colIndex)
            paint.color = item.color
            
            val r = item.distance * maxRadius
            val angleRad = Math.toRadians((rotation + item.angleOffset).toDouble())
            
            val x = cx + cos(angleRad).toFloat() * r
            val y = cy + sin(angleRad).toFloat() * r
            
            // Draw Item
            // Stretch based on speed for motion blur look?
            // Simple circle for now
            bufCanvas.drawCircle(x, y, item.size, paint)
        }
        
        // Draw Center "Agitator"
        paint.color = Color.DKGRAY
        paint.alpha = 100
        bufCanvas.drawCircle(cx, cy, maxRadius * 0.2f, paint)
        
        // 5. Blit
        canvas.drawBitmap(buffer!!, 0f, 0f, null)
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
        bufferCanvas = null
    }
}
