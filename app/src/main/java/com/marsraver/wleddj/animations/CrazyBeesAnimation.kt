package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import kotlin.random.Random

/**
 * Crazy Bees animation - Bees flying to random targets.
 * Migrated to Canvas (Vector) rendering for high-res smoothness.
 */
class CrazyBeesAnimation : Animation {

    // --- Animation Interface ---
    private var _palette: Palette = Palettes.getDefault()
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = false // Random colors used internally

    // --- State ---
    private var buffer: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    
    // Params
    private var paramSpeed: Int = 128
    
    // Tools
    private val paint = Paint().apply { isAntiAlias = true }
    private val fadePaint = Paint().apply { 
        color = Color.BLACK 
        // DST_OUT fades Alpha: Result = Dest * (1 - SourceAlpha)
        // Alpha 40/255 ~= 15% fade per frame
        alpha = 40 
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
    }
    private val clearRect = Rect()

    private class Bee {
        var x: Float = 0f
        var y: Float = 0f
        var aimX: Float = 0f
        var aimY: Float = 0f
        var color: Int = 0
        
        fun pickTarget(w: Float, h: Float, random: Random) {
            aimX = random.nextFloat() * w
            aimY = random.nextFloat() * h
            
            // Random color
            color = android.graphics.Color.HSVToColor(floatArrayOf(random.nextFloat() * 360f, 1f, 1f))
        }
        
        fun update(w: Float, h: Float, speed: Float) {
            val dx = aimX - x
            val dy = aimY - y
            val dist = kotlin.math.sqrt(dx*dx + dy*dy)
            
            if (dist < speed) {
                // Reached target
                x = aimX
                y = aimY
                pickTarget(w, h, Random.Default)
            } else {
                // Move towards target
                x += (dx / dist) * speed
                y += (dy / dist) * speed
            }
        }
    }

    private val bees = mutableListOf<Bee>()
    private val maxBees = 5
    
    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // 1. Manage Buffer
        val w = width.toInt().coerceAtLeast(1)
        val h = height.toInt().coerceAtLeast(1)
        
        if (buffer == null || buffer?.width != w || buffer?.height != h) {
            buffer?.recycle()
            buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(buffer!!)
            
            // Clear new buffer
            bufferCanvas?.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            
            // Bees need to adjust to new bounds if resize happened
            if (bees.isNotEmpty()) {
                // If drastic resize, respawn. If minor, keep.
                // For simplicity, just let them fly.
            }
        }
        
        val bufCanvas = bufferCanvas ?: return
        
        // 2. Initialize Bees if needed
        if (bees.isEmpty()) {
            repeat(maxBees) {
                bees.add(Bee().apply {
                    x = Random.nextFloat() * width
                    y = Random.nextFloat() * height
                    pickTarget(width, height, Random.Default)
                })
            }
        }
        
        // 3. Fade Trails
        // Draw a semi-transparent black rect over the whole buffer
        clearRect.set(0, 0, w, h)
        bufCanvas.drawRect(clearRect, fadePaint)
        
        // 4. Update and Draw Bees
        
        // Speed Calculation:
        // Base = 5f. Max = 20f. (Pixels per frame)
        // Adjust this to taste.
        // paramSpeed: 0..255
        val speed = 2f + (paramSpeed / 255f) * 15f
        
        for (bee in bees) {
            bee.update(width, height, speed)
            
            // Draw Flower (Target)
            // Draw Flower (Target)
            // Draw Flower (Target)
            paint.color = bee.color
            paint.style = Paint.Style.FILL
            
            // Draw 8 elongated petals
            val petalLength = 12f
            val petalWidth = 5f
            val centerOffset = 5f
            
            for (i in 0 until 8) {
                bufCanvas.save()
                val angle = i * 45f
                bufCanvas.rotate(angle, bee.aimX, bee.aimY)
                // Draw oval relative to center
                // Oval extends from offset outwards
                val oval = android.graphics.RectF(
                    bee.aimX - petalWidth/2, 
                    bee.aimY + centerOffset, 
                    bee.aimX + petalWidth/2, 
                    bee.aimY + centerOffset + petalLength
                )
                bufCanvas.drawOval(oval, paint)
                bufCanvas.restore()
            }
            
            // Center Dot (White)
            paint.color = Color.WHITE 
            bufCanvas.drawCircle(bee.aimX, bee.aimY, 5f, paint)
            
            // Draw Bee
            paint.style = Paint.Style.FILL
            val beeRadius = 6f
            bufCanvas.drawCircle(bee.x, bee.y, beeRadius, paint)
        }
        
        // 5. Blit Buffer to Screen
        canvas.drawBitmap(buffer!!, 0f, 0f, null)
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
        bufferCanvas = null
    }
}
