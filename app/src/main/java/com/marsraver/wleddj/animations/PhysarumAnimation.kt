package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PhysarumAnimation : Animation {

    override fun supportsPrimaryColor(): Boolean = false
    override var primaryColor: Int = Color.WHITE
    override fun supportsSecondaryColor(): Boolean = false
    override var secondaryColor: Int = Color.BLACK
    override fun supportsPalette(): Boolean = true
    // Default to CYTOPLASMIC for that organic Slime look
    override var currentPalette: Palette? = Palette.CYTOPLASMIC

    private val WIDTH = 64
    private val HEIGHT = 64
    private val NUM_AGENTS = 500
    
    private val trailMap = FloatArray(WIDTH * HEIGHT)
    private val diffuseMap = FloatArray(WIDTH * HEIGHT)
    private val agents = Array(NUM_AGENTS) { Agent() }
    
    private val sensorAngle = Math.PI / 4
    private val sensorDist = 5.0
    private val turnAngle = Math.PI / 4
    private val decayRate = 0.95f
    
    // Params
    private var moveSpeed = 1.0
    private var paramSpeed: Int = 128
    
    // Speed Support
    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Float) {
        paramSpeed = (speed * 255f).toInt().coerceIn(0, 255)
        // Map 0..255 -> 0.2 .. 3.0
        moveSpeed = 0.2 + (paramSpeed / 255.0) * 2.8
    }
    override fun getSpeed(): Float = paramSpeed / 255f
    
    // reusable objects
    private val paint = Paint()
    private val bitmap = android.graphics.Bitmap.createBitmap(WIDTH, HEIGHT, android.graphics.Bitmap.Config.ARGB_8888)
    private val pixels = IntArray(WIDTH * HEIGHT)
    private val matrix = android.graphics.Matrix()

    private class Agent {
        var x = Random.nextDouble() * 64
        var y = Random.nextDouble() * 64
        var angle = Random.nextDouble() * Math.PI * 2
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // 1. Update Agents
        for (agent in agents) {
            updateAgent(agent)
        }

        // 2. Diffuse and Decay Trail Map
        diffuseAndDecay()

        // 3. Render map to bitmap
        for (i in trailMap.indices) {
            val value = trailMap[i]
            if (value > 0.01f) {
                val v = if (value > 1f) 1.0 else value.toDouble()
                
                if (currentPalette != null) {
                    pixels[i] = currentPalette!!.getColorAt(v).toInt()
                } else {
                    // Fallback to Grayscale if no palette (shouldn't happen often)
                    val brightness = (v * 255).toInt()
                    pixels[i] = (0xFF shl 24) or (brightness shl 16) or (brightness shl 8) or brightness
                }
            } else {
                 pixels[i] = Color.BLACK
            }
        }
        
        bitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        
        // Scale to canvas
        matrix.reset()
        matrix.setScale(width / WIDTH, height / HEIGHT)
        paint.isFilterBitmap = true
        canvas.drawBitmap(bitmap, matrix, paint)
    }

    private fun updateAgent(agent: Agent) {
        val width = WIDTH.toDouble()
        val height = HEIGHT.toDouble()
        
        // Sensors
        val xc = agent.x + cos(agent.angle) * sensorDist
        val yc = agent.y + sin(agent.angle) * sensorDist
        
        val xl = agent.x + cos(agent.angle - sensorAngle) * sensorDist
        val yl = agent.y + sin(agent.angle - sensorAngle) * sensorDist
        
        val xr = agent.x + cos(agent.angle + sensorAngle) * sensorDist
        val yr = agent.y + sin(agent.angle + sensorAngle) * sensorDist
        
        val vc = sense(xc, yc)
        val vl = sense(xl, yl)
        val vr = sense(xr, yr)
        
        // Steer
        if (vc > vl && vc > vr) {
            // Forward is best, do nothing
        } else if (vc < vl && vc < vr) {
            // Forward is worst, random turn
            agent.angle += (Random.nextDouble() - 0.5) * 2 * turnAngle
        } else if (vl > vr) {
            // Left is best
            agent.angle -= turnAngle
        } else if (vr > vl) {
            // Right is best
            agent.angle += turnAngle
        }
        
        // Move
        agent.x += cos(agent.angle) * moveSpeed
        agent.y += sin(agent.angle) * moveSpeed
        
        // Formatting: Wrap around
        if (agent.x < 0) agent.x += width
        if (agent.x >= width) agent.x -= width
        if (agent.y < 0) agent.y += height
        if (agent.y >= height) agent.y -= height
        
        // Deposit trail
        val ix = agent.x.toInt()
        val iy = agent.y.toInt()
        if (ix in 0 until WIDTH && iy in 0 until HEIGHT) {
            trailMap[iy * WIDTH + ix] = (trailMap[iy * WIDTH + ix] + 0.5f).coerceAtMost(1.0f)
        }
    }
    
    private fun sense(x: Double, y: Double): Float {
        var ix = x.toInt()
        var iy = y.toInt()
        
        // Wrap for sensing too
        if (ix < 0) ix += WIDTH
        if (ix >= WIDTH) ix -= WIDTH
        if (iy < 0) iy += HEIGHT
        if (iy >= HEIGHT) iy -= HEIGHT
        
        return trailMap[iy * WIDTH + ix]
    }
    
    private fun diffuseAndDecay() {
        // Simple 3x3 box blur kernel
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                var sum = 0f
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        var sx = x + dx
                        var sy = y + dy
                        
                        // Wrap
                        if (sx < 0) sx += WIDTH
                        if (sx >= WIDTH) sx -= WIDTH
                        if (sy < 0) sy += HEIGHT
                        if (sy >= HEIGHT) sy -= HEIGHT
                        
                        sum += trailMap[sy * WIDTH + sx]
                    }
                }
                diffuseMap[y * WIDTH + x] = sum / 9.0f * decayRate
            }
        }
        
        // Swap buffers (or just copy back, here we copy back since agents write to trailMap directly next frame)
        System.arraycopy(diffuseMap, 0, trailMap, 0, trailMap.size)
    }

    override fun onTouch(touchX: Float, touchY: Float): Boolean {
        // Disturb/Attract agents?
        // Let's re-randomize them for chaos
        for (agent in agents) {
            agent.x = Random.nextDouble() * WIDTH
            agent.y = Random.nextDouble() * HEIGHT
            agent.angle = Random.nextDouble() * Math.PI * 2
        }
        // Clear trail
        trailMap.fill(0f)
        return true
    }
}
