package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.pow
import kotlin.random.Random

class InfiniteTunnelAnimation : Animation {

    private val loudnessMeter = LoudnessMeter()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.SQUARE
    }

    private var _palette: Palette = Palette.RAINBOW
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    // State
    // We simulate movement in Z space.
    // Z ranges from Far (e.g. 10.0) to Near (0.1)
    // High Contrast Tune: Fewer segments = bigger gaps
    private val numSegments = 12
    private val zPositions = FloatArray(numSegments) { i -> 
         // Distribute initially
         10.0f * (i + 1) / numSegments.toFloat()
    }
    
    private var speed = 0.05f
    private var lastVolume = 0f
    private var time = 0f
    
    // Rotating effect
    private var rotation = 0f

    override fun supportsPrimaryColor(): Boolean = false
    override fun supportsSecondaryColor(): Boolean = false
    override fun supportsPalette(): Boolean = true

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val cx = width / 2f
        val cy = height / 2f
        
        // Audio Logic
        val volume = loudnessMeter.getNormalizedLoudness() / 1024f
        val beat = volume > 0.1f && volume > lastVolume * 1.1f
        lastVolume = volume
        
        // Audio Speed Boost
        // Chill Mode: 0.02 base (was 0.04), 0.1 mult (was 0.2)
        val targetSpeed = 0.02f + (volume * 0.1f)
        speed = speed * 0.95f + targetSpeed * 0.05f // Slower smoothing

        // Rotate slightly on beat or constant
        rotation += (0.1f + volume * 1.0f) 

        // Update Time
        time += 0.005f + (volume * 0.02f) 

        // Update Z
        for (i in zPositions.indices) {
            zPositions[i] -= speed
            if (zPositions[i] < 0.1f) {
                zPositions[i] += 10.0f // Recycle to back
            }
        }
        
        // Occlusion Logic: Near to Far with Clipping
        // This simulates solid walls. We draw the nearest ring, then restrict
        // future drawing to ONLY be inside that ring.
        val sortedZ = zPositions.clone().sorted() // Near (Small Z) to Far (Large Z)

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(rotation)
        
        for (z in sortedZ) {
            val scale = 10.0f / z 
            
            val curveFreq = 0.5f
            val curveAmp = width * 0.5f 
            
            val shiftX = kotlin.math.sin(time + z * curveFreq) * curveAmp * (z / 10.0f)
            val shiftY = kotlin.math.cos(time * 0.7f + z * curveFreq) * curveAmp * (z / 10.0f)
            
            val size = (Math.max(width, height) * 0.1f) * scale
            val halfSize = size / 2f
            
            // Super High Contrast: Thinner lines (was 3f/25f)
            paint.strokeWidth = (1.5f * scale).coerceAtMost(12f) 
            
            val alphaPred = (1.0f - (z / 10.0f)).coerceIn(0f, 1f) 
            
            val colorIndex = ((10.0f - z) * 20 + time * 50).toInt() 
            val color = _palette.getInterpolatedInt(colorIndex % 255)
            
            paint.color = color
            paint.alpha = (alphaPred * 255).toInt()
            
            val drawX = shiftX
            val drawY = shiftY
            
            // Draw the Segmment
            val left = drawX - halfSize
            val top = drawY - halfSize
            val right = drawX + halfSize
            val bottom = drawY + halfSize
            
            canvas.drawRect(left, top, right, bottom, paint)
            
            // CLIP!
            // Restrict next segments to be INSIDE this one.
            // This hides parts that would be "behind the wall".
            // We inset slightly by stroke width so we don't clip OURSELF if we were filled?
            // Actually for wireframe, we just clip to the box.
            // Note: clipRect intersects with current clip.
            canvas.clipRect(left, top, right, bottom)
        }
        
        canvas.restore()
    }

    override fun destroy() {
        loudnessMeter.stop()
    }
}
