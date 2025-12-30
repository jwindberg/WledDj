package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.sin

/**
 * Swirl Animation - Vector Symmetrical Orbs.
 */
class SwirlAnimation : Animation {

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
        // DST_OUT fades Alpha: Result = Dest * (1 - SourceAlpha)
        alpha = 20 // Long trails
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
    }
    private val clearRect = Rect()
    
    // Audio
    private var loudnessMeter: LoudnessMeter? = null
    private var isInit = false

    // Params
    private var paramSpeed: Int = 128
    private var timeSeconds: Double = 0.0

    private fun init() {
        if (!isInit) {
            loudnessMeter = LoudnessMeter()
            isInit = true
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        init()
        val w = width.toInt().coerceAtLeast(1)
        val h = height.toInt().coerceAtLeast(1)
        
        // 1. Buffer
        if (buffer == null || buffer?.width != w || buffer?.height != h) {
            buffer?.recycle()
            buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(buffer!!)
            bufferCanvas?.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }
        val bufCanvas = bufferCanvas ?: return
        
        // 2. Fade
        clearRect.set(0, 0, w, h)
        bufCanvas.drawRect(clearRect, fadePaint)
        
        // 3. Update
        val speed = 0.01 + (paramSpeed / 255.0) * 0.04
        timeSeconds += speed
        
        val loudness = loudnessMeter?.getNormalizedLoudness() ?: 0
        val lFloat = loudness.toFloat()
        // Reduced size by half (was / 20f, now / 40f)
        val radiusBoost = (lFloat / 30f).coerceIn(0f, 10f) 
        val baseRadius = minOf(width, height) / 40f
        
        // Lissajous Coordinates
        // Frequency logic from original: freq1 = 27*speed, freq2=41*speed
        // approximating that ratio for chaos:
        val t = timeSeconds * 2.0
        val xVal = (sin(t * 1.0) + 1) / 2.0 // 0..1
        val yVal = (sin(t * 1.51) + 1) / 2.0 // 0..1 (ratio 1.5ish)
        
        val padding = baseRadius * 2
        val uw = width - padding*2
        val uh = height - padding*2
        
        val i = padding + xVal * uw // X coord (0..width)
        val j = padding + yVal * uh // Y coord (0..height)
        
        val ni = width - i
        val nj = height - j
        
        // Draw 6 Orbs (Symmetry)
        // 1. i, j
        drawOrb(bufCanvas, i.toFloat(), j.toFloat(), baseRadius + radiusBoost, 0)
        // 2. j, i (Swap X/Y - might clip if non-square, but let's allow it for "swirl" effect)
        drawOrb(bufCanvas, j.toFloat(), i.toFloat(), baseRadius + radiusBoost, 1)
        // 3. ni, nj
        drawOrb(bufCanvas, ni.toFloat(), nj.toFloat(), baseRadius + radiusBoost, 2)
        // 4. nj, ni
        drawOrb(bufCanvas, nj.toFloat(), ni.toFloat(), baseRadius + radiusBoost, 3)
        // 5. i, nj
        drawOrb(bufCanvas, i.toFloat(), nj.toFloat(), baseRadius + radiusBoost, 4)
        // 6. ni, j
        drawOrb(bufCanvas, ni.toFloat(), j.toFloat(), baseRadius + radiusBoost, 5)
        
        // 4. Blit
        canvas.drawBitmap(buffer!!, 0f, 0f, null)
    }
    
    private fun drawOrb(c: Canvas, x: Float, y: Float, r: Float, index: Int) {
        val colorIndex = ((timeSeconds * 50) + index * 40).toInt() % 256
        paint.color = _palette.getInterpolatedInt(colorIndex)
        paint.style = Paint.Style.FILL
        c.drawCircle(x, y, r, paint)
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
        bufferCanvas = null
        loudnessMeter?.stop()
        loudnessMeter = null
    }
}
