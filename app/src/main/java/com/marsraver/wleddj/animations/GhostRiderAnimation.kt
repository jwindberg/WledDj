package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ghost Rider Animation - Vector Sparkler.
 */
class GhostRiderAnimation : Animation {

    private var _palette: Palette = Palettes.get("Rainbow") ?: Palettes.getDefault()
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
        alpha = 30
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
    }
    private val clearRect = Rect()
    
    // Head logic
    private var headX: Float = 0f
    private var headY: Float = 0f
    private var angle: Float = 0f
    private var angleSpeed: Float = 2.0f
    
    // Particles
    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var life: Float = 1.0f,
        var color: Int
    )
    private val particles = mutableListOf<Particle>()
    
    // Params
    private var paramSpeed: Int = 128
    private var paramIntensity: Int = 128 // Used for blur/trail amount? or Particle count?

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val w = width.toInt().coerceAtLeast(1)
        val h = height.toInt().coerceAtLeast(1)
        
        // Init Head
        if (headX == 0f) {
            headX = width / 2f
            headY = height / 2f
            angle = Random.nextFloat() * 360f
        }
        
        // 1. Buffer
        if (buffer == null || buffer?.width != w || buffer?.height != h) {
            buffer?.recycle()
            buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(buffer!!)
            bufferCanvas?.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }
        val bufCanvas = bufferCanvas ?: return
        
        // 2. Fade
        // Map intensity to trail length?
        // Higher intensity -> Less fade -> Longer trails
        val fadeAlpha = 60 - (paramIntensity / 255f * 50).toInt()
        fadePaint.alpha = fadeAlpha.coerceIn(5, 255)
        clearRect.set(0, 0, w, h)
        bufCanvas.drawRect(clearRect, fadePaint)
        
        // 3. Update Head
        val speed = 2f + (paramSpeed / 255f) * 8f
        
        // Move in curvy path
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        headX += cos(rad) * speed
        headY += sin(rad) * speed
        
        angle += angleSpeed
        if (Random.nextFloat() < 0.05f) {
             angleSpeed = (Random.nextFloat() - 0.5f) * 10f
        }
        
        // Wrap
        if (headX < 0) headX = width
        if (headX > width) headX = 0f
        if (headY < 0) headY = height
        if (headY > height) headY = 0f
        
        // Draw Head
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        bufCanvas.drawCircle(headX, headY, 5f, paint)
        
        // 4. Emit Particles
        val particleCount = 2 // emit per frame
        for (i in 0 until particleCount) {
             val pSpeed = Random.nextFloat() * 2f
             val pAngle = Random.nextFloat() * 6.28f
             val colorIndex = (System.currentTimeMillis() / 10).toInt() % 256
             
             particles.add(Particle(
                 x = headX, y = headY,
                 vx = cos(pAngle) * pSpeed,
                 vy = sin(pAngle) * pSpeed,
                 life = 1.0f,
                 color = _palette.getInterpolatedInt(colorIndex)
             ))
        }
        
        // 5. Update Particles
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx
            p.y += p.vy
            
            p.life -= 0.02f // Fade out rate
            
            p.vx *= 0.95f // Drag
            p.vy *= 0.95f
            
            if (p.life <= 0f) {
                iter.remove()
            } else {
                paint.color = p.color
                // Alpha based on life
                paint.alpha = (p.life * 255).toInt()
                bufCanvas.drawCircle(p.x, p.y, 3f * p.life, paint)
            }
        }
        
        // 6. Blit
        canvas.drawBitmap(buffer!!, 0f, 0f, null)
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
        bufferCanvas = null
    }
}
