package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.math.MathUtils
import kotlin.math.min

/**
 * PlasmaBall2D - 2D plasma ball effect
 * Migrated to WledDj.
 */
class PlasmaBallAnimation : BasePixelAnimation() {

    override fun supportsPalette(): Boolean = true
    override fun getDefaultPalette(): com.marsraver.wleddj.engine.color.Palette = com.marsraver.wleddj.engine.color.Palette.ICE

    // Speed Support
    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        paramSpeed = (speed * 255f).toInt().coerceIn(0, 255)
    }
    override fun getSpeed(): Float = paramSpeed / 255f

    private var startTimeNs: Long = 0L
    private val paint = android.graphics.Paint()
    private val path = android.graphics.Path()

    // Lightning Targets (Bolts)
    private data class Bolt(var angle: Float, var speed: Float, var phase: Float)
    private val bolts = mutableListOf<Bolt>()
    private val rand = java.util.Random()
    
    private var isInitialized = false

    override fun onInit() {
        startTimeNs = System.nanoTime()
        bolts.clear()
        // Spawn 7 bolts
        for (i in 0 until 7) {
            bolts.add(Bolt(
                angle = rand.nextFloat() * 360f,
                speed = (rand.nextFloat() - 0.5f) * 2f, // -1 to 1 rot speed
                phase = rand.nextFloat() * 100f
            ))
        }
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = Math.max(1, now)
        return true
    }
    
    override fun draw(canvas: android.graphics.Canvas, width: Float, height: Float) {
        this.width = width.toInt()
        this.height = height.toInt()
        
        if (!isInitialized) {
            onInit()
            isInitialized = true
        }

        // Clear
        canvas.drawColor(android.graphics.Color.BLACK)
        
        val cx = width / 2
        val cy = height / 2
        val maxRadius = kotlin.math.min(width, height) / 2 * 0.95f
        
        // Time
        val timeSec = (System.nanoTime() - startTimeNs) / 1_000_000_000f
        // Speed Factor: 0.1 .. 5.0
        val spdFactor = 0.1f + (paramSpeed / 255f) * 4.9f
        
        // Setup Paint for Lightning (INNER CORE)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeCap = android.graphics.Paint.Cap.ROUND
        paint.isAntiAlias = true
        
        // Bolt Loop
        // We draw two passes: Glow (thick/transparent) and Core (thin/bright)
        
        // 1. Update Bolts
        for (b in bolts) {
            b.angle += b.speed * spdFactor
            // Wiggle phase
            b.phase += spdFactor * 0.1f
        }
        
        // 2. Draw Glow
        paint.strokeWidth = 15f
        paint.alpha = 50
        drawBolts(canvas, cx, cy, maxRadius, timeSec, true)
        
        // 3. Draw Core
        paint.strokeWidth = 3f
        paint.alpha = 255
        drawBolts(canvas, cx, cy, maxRadius, timeSec, false)
        
        // 4. Center Electrode
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.DKGRAY
        canvas.drawCircle(cx, cy, 15f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.alpha = 200
        canvas.drawCircle(cx, cy, 5f, paint)
    }
    
    private fun drawBolts(canvas: android.graphics.Canvas, cx: Float, cy: Float, radius: Float, time: Float, isGlow: Boolean) {
        for ((index, b) in bolts.withIndex()) {
            val rad = Math.toRadians(b.angle.toDouble())
            val ex = cx + Math.cos(rad).toFloat() * radius
            val ey = cy + Math.sin(rad).toFloat() * radius
            
            // Palette Color
            // Cycle slowly over time + offset by bolt index
            val colorIdx = ((time * 20 + index * 30).toInt()) % 256
            val color = getColorFromPalette(colorIdx)
            
            paint.color = if (isGlow) (color and 0x00FFFFFF) or 0x40000000 else color
            
            // Recursive Lightning
            path.reset()
            path.moveTo(cx, cy)
            
            // Jitter amount based on distance
            val displacement = 40f
            
            generateLightning(cx, cy, ex, ey, displacement, path)
            canvas.drawPath(path, paint)
        }
    }
    
    // Recursive midpoint displacement
    private fun generateLightning(x1: Float, y1: Float, x2: Float, y2: Float, displace: Float, path: android.graphics.Path) {
        if (displace < 2f) {
            path.lineTo(x2, y2)
        } else {
            val midX = (x1 + x2) / 2
            val midY = (y1 + y2) / 2
            
            // Normal vector
            val dx = x2 - x1
            val dy = y2 - y1
            // Orthogonal: -dy, dx
            // Random offset
            val scale = (rand.nextFloat() - 0.5f) * displace
            val nx = midX - dy * 0.0f + scale // Simplified jitter just adds to coord? 
            // Better: displace along normal is overkill, just displace XY randomly
            val jx = midX + (rand.nextFloat() - 0.5f) * displace
            val jy = midY + (rand.nextFloat() - 0.5f) * displace
            
            generateLightning(x1, y1, jx, jy, displace / 2, path)
            generateLightning(jx, jy, x2, y2, displace / 2, path)
        }
    }
}
