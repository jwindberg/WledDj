package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Space Ships animation - Vector based.
 * Ships fly with steering behavior and leave trails.
 */
class SpaceShipsAnimation : Animation {

    private var _palette: Palette = Palette.fromName("Rainbow") ?: Palette.DEFAULT
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
        alpha = 25 // Trails length
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
    }
    private val shipPath = Path()
    private val clearRect = Rect()

    private class Ship {
        var x: Float = 0f
        var y: Float = 0f
        var vx: Float = 0f
        var vy: Float = 0f
        
        var color: Int = Color.WHITE
        var wanderTheta: Float = 0f
        
        fun update(w: Float, h: Float, speed: Float) {
            // Steering / Wander
            // Change wander angle slightly
            wanderTheta += (Random.nextFloat() - 0.5f) * 0.5f
            
            // Calculate velocity vector from angle
            val speedX = cos(wanderTheta) * speed
            val speedY = sin(wanderTheta) * speed
            
            // Softly steer towards new velocity
            vx += (speedX - vx) * 0.1f
            vy += (speedY - vy) * 0.1f
            
            x += vx
            y += vy
            
            if (x < 0) x = w
            if (x > w) x = 0f
            if (y < 0) y = h
            if (y > h) y = 0f
        }
    }

    private val ships = mutableListOf<Ship>()
    private val SHIP_COUNT = 8
    
    // Params
    private var paramSpeed: Int = 128

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val w = width.toInt().coerceAtLeast(1)
        val h = height.toInt().coerceAtLeast(1)
        
        // 1. Buffer Management
        if (buffer == null || buffer?.width != w || buffer?.height != h) {
            buffer?.recycle()
            buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(buffer!!)
            
            // Clear
            bufferCanvas?.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            
            ships.clear()
        }
        
        val bufCanvas = bufferCanvas ?: return
        
        // 2. Init Ships
        if (ships.isEmpty()) {
            repeat(SHIP_COUNT) {
                ships.add(Ship().apply {
                    x = Random.nextFloat() * width
                    y = Random.nextFloat() * height
                    vx = (Random.nextFloat() - 0.5f) * 5f
                    vy = (Random.nextFloat() - 0.5f) * 5f
                    wanderTheta = Random.nextFloat() * 6.28f
                })
            }
        }
        
        // 3. Fade Trails
        clearRect.set(0, 0, w, h)
        bufCanvas.drawRect(clearRect, fadePaint)
        
        // 4. Update & Draw Ships
        val speed = 2f + (paramSpeed / 255f) * 10f
        val time = System.currentTimeMillis()
        
        ships.forEachIndexed { index, ship ->
            ship.update(width, height, speed)
            
            // Color from Palette
            // Cycle colors over time + index offset
            val colorIndex = (time / 20 + index * 30).toInt() % 256
            ship.color = _palette.getInterpolatedInt(colorIndex)
            
            // Orientation
            val angle = atan2(ship.vy, ship.vx) * (180f / Math.PI.toFloat()) + 90f
            
            bufCanvas.save()
            bufCanvas.translate(ship.x, ship.y)
            bufCanvas.rotate(angle)
            
            // Draw Ship (Triangle)
            // Tip at (0, -10), Base at (-6, 6) and (6, 6)
            shipPath.reset()
            shipPath.moveTo(0f, -12f)
            shipPath.lineTo(-7f, 8f)
            shipPath.lineTo(0f, 5f) // Indent at engine
            shipPath.lineTo(7f, 8f)
            shipPath.close()
            
            paint.style = Paint.Style.FILL
            paint.color = ship.color
            bufCanvas.drawPath(shipPath, paint)
            
            // Engine Glow
            paint.color = Color.CYAN // Or Orange?
            bufCanvas.drawCircle(0f, 7f, 3f, paint)
            
            bufCanvas.restore()
        }
        
        // 5. Blit
        canvas.drawBitmap(buffer!!, 0f, 0f, null)
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
        bufferCanvas = null
    }
}
