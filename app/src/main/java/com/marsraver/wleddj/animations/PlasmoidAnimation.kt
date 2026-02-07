package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import kotlin.math.min

/**
 * Plasmoid animation - Plasma-like effect with audio reactivity
 * Migrated to WledDj.
 */
import com.marsraver.wleddj.engine.color.Palette

class PlasmoidAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true
    override fun getDefaultPalette(): Palette = Palette.RAINBOW

    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L
    private var timeAccumulator: Double = 0.0

    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        val old = paramSpeed
        paramSpeed = (speed * 255).toInt()
        System.out.println("Plasmoid: setSpeed($speed) -> paramSpeed=$paramSpeed (was $old)")
    }
    override fun getSpeed(): Float = paramSpeed / 255f
    
    override fun onInit() {
        startTimeNs = System.nanoTime()
        loudnessMeter = LoudnessMeter()
        timeAccumulator = 0.0
    }

    override fun draw(canvas: android.graphics.Canvas, width: Float, height: Float) {
        // Force higher resolution for smoother/more detailed plasma
        // 100x100 is good balance of detail vs CPU
        if (this.width != 100 || this.height != 100) {
            init(100, 100)
        }
        
        super.draw(canvas, width, height)
    }

    override fun update(now: Long): Boolean {
        // Fix Time Base: Ignore 'now' (which is Millis from Base) and use NanoTime explicitly
        val realNow = System.nanoTime()
        if (startTimeNs == 0L) startTimeNs = realNow
        val dt = (realNow - startTimeNs) / 1_000_000_000.0 // Seconds
        startTimeNs = realNow
        
        // Speed Control
        // 0.1x to 2.0x speed (Slower, less overwhelming)
        val speedFactor = 0.1 + (paramSpeed / 255.0) * 1.9
        timeAccumulator += dt * speedFactor

        val t = timeAccumulator

        // Audio Reactivity
        val loudness = loudnessMeter?.getNormalizedLoudness() ?: 0
        // Scale brightness: 0..1.0
        val audioBoost = (loudness / 1024.0 * 1.5).coerceIn(0.0, 1.5)

        // Center 1: Circular motion
        val cx1 = width * (0.5 + 0.3 * Math.sin(t * 0.7))
        val cy1 = height * (0.5 + 0.3 * Math.cos(t * 0.6))
        
        // Center 2: Figure-8 motion
        val cx2 = width * (0.5 + 0.4 * Math.sin(t * 0.8))
        val cy2 = height * (0.5 + 0.2 * Math.sin(t * 1.5))
        
        // Scale: How tightly packed the ripples are
        val scale = 0.3 

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Dist to center 1
                val d1 = Math.sqrt((x-cx1)*(x-cx1) + (y-cy1)*(y-cy1))
                // Dist to center 2
                val d2 = Math.sqrt((x-cx2)*(x-cx2) + (y-cy2)*(y-cy2))
                
                // Ripple 1 (Expanding)
                val v1 = Math.sin(d1 * scale - t * 2.0)
                
                // Ripple 2 (Expanding)
                val v2 = Math.sin(d2 * scale - t * 2.2)
                
                // Interference
                val v = (v1 + v2) / 2.0 // -1..1
                
                // Map to Palette
                // Sharp sine map for distinct rings?
                // Or smooth? 
                // Let's try to map (-1..1) -> (0..255) wrapping? 
                // actually (v+1)/2 is 0..1.
                
                val flow = v
                val colorIndex = (((flow + 1.0) / 2.0 + audioBoost) * 255.0).toInt() % 256
                
                val color = getColorFromPalette(colorIndex)
                setPixelColor(x, y, color)
            }
        }
        return true
    }

    override fun destroy() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }
}
