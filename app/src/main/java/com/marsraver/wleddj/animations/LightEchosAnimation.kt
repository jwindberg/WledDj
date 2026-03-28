package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.audio.LoudnessMeter
import kotlin.random.Random
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos

/**
 * Light Echos (Node Ripple Network V5.2 - Colorful & Drifting)
 * 
 * Nodes and the global beat center slowly organically drift around the room
 * in Lissajous curves, adding immense life to the fluid vector ripples.
 * Every node inherits a unique color phase from the active palette, producing
 * an enormously colorful, psychedelic interference pattern.
 */
class LightEchosAnimation : BasePixelAnimation() {

    override fun supportsPrimaryColor(): Boolean = false
    override fun supportsPalette(): Boolean = true
    override fun supportsSpeed(): Boolean = true

    // --- Core State ---
    private data class Node(
        val baseX: Float, 
        val baseY: Float,
        val phaseX: Float,
        val phaseY: Float,
        val speedX: Float,
        val speedY: Float,
        val colorPhase: Float,
        var lastTriggerTime: Long = 0L
    ) {
        fun currentX(time: Float, width: Float): Float = baseX + sin(time * speedX + phaseX) * (width * 0.2f)
        fun currentY(time: Float, height: Float): Float = baseY + cos(time * speedY + phaseY) * (height * 0.2f)
    }
    
    private data class Ripple(
        val x: Float, 
        val y: Float, 
        val color: Int, 
        var radius: Float = 0f, 
        val maxRadius: Float,
        val speed: Float
    ) {
        fun update() { radius += speed }
        val isAlive get() = radius < maxRadius
        val progress get() = radius / maxRadius
    }

    private val nodes = mutableListOf<Node>()
    private val activeRipples = mutableListOf<Ripple>()
    
    private var waveX = 0f
    private var waveDx = 3f
    private var paletteIndex = 0f
    private var time = 0f

    // --- Rendering ---
    private val ripplePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val nodePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        alpha = 30 
    }
    
    // Fast clear keeps rings sharp and prevents blurry muddy artifacts,
    // enhancing the colorful interference lines.
    private val bgPaint = Paint().apply {
        color = Color.BLACK
        alpha = 50 
    }

    private val loudnessMeter = LoudnessMeter()

    override fun onInit() {
        nodes.clear()
        activeRipples.clear()
        
        // Scatter nodes across the canvas
        // Each node gets a unique color from the palette to explode the color space
        for (i in 0..40) {
            nodes.add(Node(
                baseX = Random.nextFloat() * width,
                baseY = Random.nextFloat() * height,
                phaseX = Random.nextFloat() * (Math.PI.toFloat() * 2),
                phaseY = Random.nextFloat() * (Math.PI.toFloat() * 2),
                speedX = 0.5f + Random.nextFloat() * 1.5f,
                speedY = 0.5f + Random.nextFloat() * 1.5f,
                colorPhase = Random.nextFloat() * 255f
            ))
        }
        
        waveX = 0f
        waveDx = 4f
        time = 0f
    }

    override fun setSpeed(speed: Float) {
        paramSpeed = (speed * 255f).toInt().coerceIn(0, 255)
    }

    override fun getSpeed(): Float = paramSpeed / 255f

    // --- Interactivity ---
    override fun onTouch(touchX: Float, touchY: Float): Boolean {
        // Drop a massive color-bomb from the touch point
        val color = getColorFromPalette(Random.nextInt(256))
        activeRipples.add(Ripple(
            x = touchX, 
            y = touchY, 
            color = color,
            maxRadius = width * 0.8f,
            speed = 2f
        ))
        
        // Trigger nodes near the touch point
        for (node in nodes) {
            val nx = node.currentX(time, width.toFloat())
            val ny = node.currentY(time, height.toFloat())
            val dx = nx - touchX
            val dy = ny - touchY
            if (dx * dx + dy * dy < (width * 0.15f) * (width * 0.15f)) {
                triggerNode(node, nx, ny, color, width * 0.3f)
            }
        }
        return true
    }

    override fun onTransform(panX: Float, panY: Float, zoom: Float, rotation: Float): Boolean {
        // Scrub the wave left/right
        waveX += panX
        waveX = waveX.coerceIn(0f, width.toFloat())
        return true
    }
    
    private fun triggerNode(node: Node, nx: Float, ny: Float, color: Int, maxRad: Float, speed: Float = 4f) {
        val now = System.currentTimeMillis()
        if (now - node.lastTriggerTime > 300) { 
            node.lastTriggerTime = now
            activeRipples.add(Ripple(
                x = nx,
                y = ny,
                color = color,
                maxRadius = maxRad,
                speed = speed
            ))
        }
    }

    override fun update(now: Long): Boolean = true

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        val normalizedLoudness = loudnessMeter.getNormalizedLoudness()
        val audioLevel = (normalizedLoudness / 800f).coerceIn(0f, 1f)
        val isBeat = normalizedLoudness > 450
        
        val speedMult = 1f + (paramSpeed / 255f) * 3f
        time += 0.01f * speedMult

        // 1. Advance the Activation Wave
        waveX += waveDx * speedMult
        if (waveX < 0) {
            waveX = 0f
            waveDx = abs(waveDx)
        } else if (waveX > width) {
            waveX = width
            waveDx = -abs(waveDx)
        }
        
        paletteIndex += 0.5f * speedMult
        if (paletteIndex > 255) paletteIndex -= 255f
        
        val waveBaseColor = getColorFromPalette(paletteIndex.toInt())

        // 2. Huge beat causes a global pulse from a drifting central source
        val globalSourceX = width / 2f + sin(time * 0.8f) * (width * 0.4f)
        val globalSourceY = height / 2f + cos(time * 0.6f) * (height * 0.4f)
        
        if (isBeat && Random.nextFloat() > 0.8f) {
            // A contrasting color for explosive beats instead of just white
            val beatColor = getColorFromPalette(((paletteIndex + 128f) % 256).toInt())
            activeRipples.add(Ripple(
                x = globalSourceX,
                y = globalSourceY,
                color = beatColor,
                maxRadius = width + height,
                speed = 15f + audioLevel * 20f
            ))
        }

        // 3. Trigger Nodes near the Activation Wave
        val activationRadius = 20f + (audioLevel * 50f)
        for (node in nodes) {
            val nx = node.currentX(time, width)
            val ny = node.currentY(time, height)
            
            // Generate node's unique cyclic color
            val nodeColor = getColorFromPalette(((paletteIndex + node.colorPhase) % 256).toInt())
            
            nodePaint.color = nodeColor
            nodePaint.alpha = 20 // Idle state
            
            if (abs(nx - waveX) < activationRadius) {
                // Determine ripple size and speed based on audio + random variance
                val maxRad = (width * 0.15f) + Random.nextFloat() * (width * 0.15f) + (audioLevel * width * 0.2f)
                val ripSpeed = 3f + Random.nextFloat() * 2f + (audioLevel * 5f)
                
                triggerNode(node, nx, ny, nodeColor, maxRad, ripSpeed)
                
                // Flash the node dot brightly
                nodePaint.alpha = 255
            }
            
            canvas.drawCircle(nx, ny, 3f, nodePaint)
        }

        // 4. Process and Draw Ripples
        val iterator = activeRipples.iterator()
        while (iterator.hasNext()) {
            val ripple = iterator.next()
            ripple.update()
            
            if (!ripple.isAlive) {
                iterator.remove()
            } else {
                val progress = ripple.progress
                val alpha = ((1.0f - progress) * 255f).toInt().coerceIn(0, 255)
                val strokeWidth = 8f * (1.0f - progress)
                
                ripplePaint.color = ripple.color
                ripplePaint.alpha = alpha
                ripplePaint.strokeWidth = strokeWidth.coerceAtLeast(1f)
                
                canvas.drawCircle(ripple.x, ripple.y, ripple.radius, ripplePaint)
            }
        }
        
        // Optional: Very faint sweeping line so you can see what is activating the waves
        ripplePaint.color = waveBaseColor
        ripplePaint.alpha = 20
        ripplePaint.strokeWidth = 2f
        canvas.drawLine(waveX, 0f, waveX, height, ripplePaint)
    }
}
