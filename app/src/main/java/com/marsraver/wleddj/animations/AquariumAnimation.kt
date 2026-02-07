package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.sin
import kotlin.random.Random

/**
 * Aquarium Animation - A peaceful underwater scene.
 * Features: Swaying plants, swimming vector fish, rising bubbles.
 */
class AquariumAnimation : Animation {

    private var _palette: Palette = Palette.RAINBOW
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true // Used for Fish colors
    override fun supportsSpeed(): Boolean = true
    
    // Speed Control
    // 0.5 input = 1.0 multiplier (Normal speed)
    // Range: 0.0 (Pause) to 2.0 (Double speed)
    private var speedMultiplier: Float = 1.0f

    override fun setSpeed(speed: Float) {
        speedMultiplier = speed * 2f
    }

    override fun getSpeed(): Float = speedMultiplier / 2f

    // State
    private var buffer: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    
    // Paints
    private val waterPaint = Paint()
    private val plantPaint = Paint().apply { 
        color = Color.rgb(34, 139, 34) // Forest Green
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val fishPaint = Paint().apply { isAntiAlias = true }
    private val bubblePaint = Paint().apply { 
        color = Color.argb(100, 255, 255, 255) // Semi-transparent white
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    // Objects
    private class Plant(val x: Float, val height: Float, val segmentCount: Int) {
        // Offset for swaying phase
        val phase = Random.nextFloat() * 6.28f 
    }
    
    private class Fish {
        var x: Float = 0f
        var y: Float = 0f
        var vx: Float = 0f
        var size: Float = 1.0f
        var color: Int = Color.RED
        var tailPhase: Float = 0f
    }
    
    private class Bubble {
        var x: Float = 0f
        var y: Float = 0f
        var vy: Float = 0f
        var size: Float = 0f
    }
    
    private val plants = mutableListOf<Plant>()
    private val fishList = mutableListOf<Fish>()
    private val bubbles = mutableListOf<Bubble>()
    
    private val plantPath = Path()
    private val fishPath = Path()

    // Time for animation
    private var lastFrameTime = 0L
    private var time = 0f

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val w = width.toInt().coerceAtLeast(1)
        val h = height.toInt().coerceAtLeast(1)
        
        // 1. Buffer Management
        if (buffer == null || buffer?.width != w || buffer?.height != h) {
            buffer?.recycle()
            buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(buffer!!)
            
            // Initialize Objects on Resize
            initWorld(width, height)
            
            // Water Gradient (Deep Blue to Lighter Blue)
            waterPaint.shader = LinearGradient(0f, 0f, 0f, height,
                Color.rgb(0, 105, 148), // Deep Sky Blue (Top)
                Color.rgb(0, 0, 80),    // Dark Navy (Bottom)
                Shader.TileMode.CLAMP
            )
        }
        val bufCanvas = bufferCanvas ?: return
        
        // 2. Clear Background (Transparent)
        bufCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        
        // Calculate Delta Time (Seconds)
        val now = System.nanoTime()
        if (lastFrameTime == 0L) lastFrameTime = now
        val dt = (now - lastFrameTime) / 1_000_000_000f
        lastFrameTime = now
        
        // Avoid huge jumps if paused
        val cleanDt = if (dt > 0.1f) 0.033f else dt

        // Base speed was 0.05 per frame @ 30fps = 1.5 per second
        time += cleanDt * 1.5f * speedMultiplier 
        
        // 3. Draw Plants (Back Layer)
        drawPlants(bufCanvas, width, height)
        
        // 4. Draw Fish
        updateAndDrawFish(bufCanvas, width, height, cleanDt)
        
        // 5. Draw Bubbles
        updateAndDrawBubbles(bufCanvas, width, height, cleanDt)
        
        // 6. Blit
        canvas.drawBitmap(buffer!!, 0f, 0f, null)
    }
    
    private fun initWorld(w: Float, h: Float) {
        plants.clear()
        // Create grass along the bottom
        val plantCount = (w / 30f).toInt().coerceIn(5, 20)
        for (i in 0 until plantCount) {
            val x = (w / plantCount) * i + Random.nextFloat() * 10f
            val height = h * (0.3f + Random.nextFloat() * 0.4f) // 30-70% height
            plants.add(Plant(x, height, 5))
        }
        
        fishList.clear()
        val fishCount = 6
        for (i in 0 until fishCount) {
            spawnFish(w, h)
        }
    }
    
    private fun spawnFish(w: Float, h: Float) {
        val f = Fish()
        f.y = Random.nextFloat() * (h * 0.8f) // Top 80%
        f.x = Random.nextFloat() * w
        f.vx = (if (Random.nextBoolean()) 1 else -1) * (1f + Random.nextFloat() * 2f)
        f.size = 15f + Random.nextFloat() * 15f
        
        // Color from Palette
        val colorIndex = Random.nextInt(256)
        f.color = _palette.getInterpolatedInt(colorIndex)
        
        fishList.add(f)
    }
    
    private fun drawPlants(c: Canvas, w: Float, h: Float) {
        for (p in plants) {
            plantPath.reset()
            plantPath.moveTo(p.x, h)
            
            val sway = sin(time + p.phase) * 20f
            
            // Bezier curve to top
            // Control point 1: 1/3 up, slight sway
            // Control point 2: 2/3 up, more sway
            // End point: Top, max sway
            
            val cp1x = p.x + sway * 0.5f
            val cp1y = h - p.height * 0.33f
            val cp2x = p.x + sway
            val cp2y = h - p.height * 0.66f
            val endX = p.x + sway * 1.5f
            val endY = h - p.height
            
            plantPath.cubicTo(cp1x, cp1y, cp2x, cp2y, endX, endY)
            
            c.drawPath(plantPath, plantPaint)
        }
    }
    
    private fun updateAndDrawFish(c: Canvas, w: Float, h: Float, dt: Float) {
        // Assume target 30fps for conversion of old units
        // Old speed: vx [pixels/frame]
        // New speed: vx * 30 [pixels/second] * dt [seconds]
        val speedScale = 30f * dt * speedMultiplier

        for (f in fishList) {
            f.x += f.vx * speedScale
            f.tailPhase += 0.5f * speedScale // Tail wag speed
            
            // Wrap
            if (f.vx > 0 && f.x > w + 50) f.x = -50f
            if (f.vx < 0 && f.x < -50) f.x = w + 50f
            
            c.save()
            c.translate(f.x, f.y)
            
            // Flip if swimming left
            if (f.vx < 0) c.scale(-1f, 1f)
            
            // Draw Body (Oval)
            fishPaint.color = f.color
            fishPaint.style = Paint.Style.FILL
            val bodyRect = android.graphics.RectF(-f.size, -f.size/2, f.size, f.size/2)
            c.drawOval(bodyRect, fishPaint)
            
            // Draw Tail (Triangle moving)
            val tailWag = sin(f.tailPhase) * (f.size * 0.2f)
            fishPath.reset()
            fishPath.moveTo(-f.size, 0f)
            fishPath.lineTo(-f.size * 1.5f, -f.size * 0.6f + tailWag)
            fishPath.lineTo(-f.size * 1.5f, f.size * 0.6f + tailWag)
            fishPath.close()
            c.drawPath(fishPath, fishPaint)
            
            // Eye
            fishPaint.color = Color.WHITE
            c.drawCircle(f.size * 0.5f, -f.size * 0.1f, f.size * 0.15f, fishPaint)
            fishPaint.color = Color.BLACK
            c.drawCircle(f.size * 0.6f, -f.size * 0.1f, f.size * 0.05f, fishPaint)
            
            c.restore()
        }
    }
    
    private fun updateAndDrawBubbles(c: Canvas, w: Float, h: Float, dt: Float) {
        val speedScale = 30f * dt * speedMultiplier
        
        // Spawn chance logic:
        // We want ~10% probability per 33ms frame.
        // Rate R = 3.0 spawns/frame-interval (roughly).
        // Let's use simpler logic: spawn chance scales with dt.
        // If dt=0.033, chance=0.1.
        // chance = 3.0 * dt.
        
        if (Random.nextFloat() < (3.0f * dt)) { 
            val b = Bubble()
            b.x = Random.nextFloat() * w
            b.y = h + 10f
            b.vy = -1f - Random.nextFloat() * 2f
            b.size = 2f + Random.nextFloat() * 4f
            bubbles.add(b)
        }
        
        val iter = bubbles.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.y += b.vy * speedScale
            // Wiggle
            b.x += sin(b.y * 0.1f) * 0.5f * speedScale 
            
            if (b.y < -10) {
                iter.remove()
            } else {
                c.drawCircle(b.x, b.y, b.size, bubblePaint)
                // Highlight
                // c.drawCircle(b.x - b.size*0.3f, b.y - b.size*0.3f, 1f, highlightPaint)
            }
        }
    }

    override fun destroy() {
        buffer?.recycle()
        buffer = null
        bufferCanvas = null
    }
}
