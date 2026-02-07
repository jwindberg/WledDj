package com.marsraver.wleddj.animations

import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import kotlin.math.sqrt
import kotlin.math.roundToInt

/**
 * PixelWave animation - Wave expanding from center based on audio
 * Migrated to WledDj.
 */
class PixelWaveAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true
    override fun getDefaultPalette(): com.marsraver.wleddj.engine.color.Palette = com.marsraver.wleddj.engine.color.Palette.OCEAN

    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    // Wave Logic
    private data class Wave(var radius: Float, val color: Int)
    private val waves = mutableListOf<Wave>()
    private var lastBeatTime: Long = 0L
    
    // Params
    // paramSpeed from BasePixelAnimation (0..255)
    // paramIntensity from BasePixelAnimation (0..255)
    
    private val paint = Paint()

    override fun onInit() {
        startTimeNs = System.nanoTime()
        loudnessMeter = LoudnessMeter()
        waves.clear()
        
        // Stroke style for rings
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 3f // Thick enough to see
        paint.isAntiAlias = true
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000

        // 1. Audio Trigger
        val loudness = loudnessMeter?.getNormalizedLoudness() ?: 0
        val volume = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        
        // Sensitvity threshold. 
        // paramIntensity controls threshold? Or line width?
        // Let's use it for threshold (Inverse: High Intensity = Low Threshold)
        val threshold = (255 - paramIntensity).coerceAtLeast(10)
        
        // Auto-beat if quiet to ensure visual activity is NOT needed if threshold works,
        // but let's keep a tiny keep-alive or just rely on normalized audio.
        // Actually, let's just use a simple threshold + cooldown.
        
        val isBeat = volume > threshold
        
        // Keep-Alive: If silent for 2s, force a wave so it's not dead.
        val nowMs = System.currentTimeMillis()
        val quiet = (nowMs - lastBeatTime) > 2000
        
        if ((isBeat || quiet) && (waves.isEmpty() || waves.last().radius > 30f)) {
             lastBeatTime = nowMs
             val colorIndex = (timeMs / 20).toInt() % 256
             val color = getColorFromPalette(colorIndex)
             waves.add(Wave(0f, color))
        }

        // 2. Update Waves
        // Speed: Map 0..255 -> 1.0 .. 20.0 pixels/frame?
        val speed = 1f + (paramSpeed / 255f) * 15f
        
        val iterator = waves.iterator()
        val maxDist = kotlin.math.sqrt((width*width + height*height).toFloat())
        
        while (iterator.hasNext()) {
            val w = iterator.next()
            w.radius += speed
            if (w.radius > maxDist) {
                iterator.remove()
            }
        }
        
        return true
    }
    
    private var isInitialized = false
    
    // Override draw completely to use Vector drawing (Circles) instead of pixel grid
    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Fix: Ensure base dimensions are updated so 'update' visible bounds are correct
        this.width = width.toInt()
        this.height = height.toInt()
        
        if (!isInitialized) {
            onInit()
            isInitialized = true
        }
    
        // Run update logic using System time
        update(System.nanoTime())
        
        // Clear background
        canvas.drawColor(android.graphics.Color.BLACK)
        
        // Draw Waves
        val cx = width / 2
        val cy = height / 2
        
        paint.style = android.graphics.Paint.Style.STROKE
        // Intensity controls line thickness?
        val strokeW = 2f + (paramIntensity / 255f) * 10f
        paint.strokeWidth = strokeW
        
        for (w in waves) {
            paint.color = w.color
            canvas.drawCircle(cx, cy, w.radius, paint)
        }
    }

    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    // We don't use expandWaveFromCenter anymore
}
