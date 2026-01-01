package com.marsraver.wleddj.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.marsraver.wleddj.engine.Animation
import kotlin.random.Random

class SnowAnimation : Animation {
    
    // Config
    private val flakeCount = 100
    private val windSpeed = 0.5f
    
    // State
    private data class Snowflake(
        var x: Float,
        var y: Float,
        var speed: Float,
        var size: Float,
        var driftOffset: Float,
        var alpha: Int
    )
    
    private val flakes = ArrayList<Snowflake>()
    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private var width = 1f
    private var height = 1f
    private var time = 0f
    
    private fun initFlakes() {
        flakes.clear()
        for (i in 0 until flakeCount) {
            flakes.add(createFlake(randomY = true))
        }
    }
    
    private fun createFlake(randomY: Boolean): Snowflake {
        return Snowflake(
            x = Random.nextFloat() * width,
            y = if (randomY) Random.nextFloat() * height else -10f,
            speed = 2f + Random.nextFloat() * 3f,
            size = 2f + Random.nextFloat() * 3f,
            driftOffset = Random.nextFloat() * 100f,
            alpha = 150 + Random.nextInt(105)
        )
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            if (flakes.isEmpty()) initFlakes()
        }
        
        if (flakes.isEmpty()) initFlakes()
        
        time += 0.01f
        
        flakes.forEach { flake ->
            // Move
            flake.y += flake.speed
            // Drift
            flake.x += kotlin.math.sin(time + flake.driftOffset) * windSpeed
            
            // Draw
            paint.alpha = flake.alpha
            canvas.drawCircle(flake.x, flake.y, flake.size, paint)
            
            // Reset if OOB
            if (flake.y > height) {
                // Reset to top
                val new = createFlake(randomY = false)
                flake.y = new.y
                flake.x = new.x
                flake.speed = new.speed
                flake.size = new.size
                flake.driftOffset = new.driftOffset
            }
            // Wrap X (optional, but good for wind)
            if (flake.x > width) flake.x = 0f
            if (flake.x < 0) flake.x = width
        }
    }
}
