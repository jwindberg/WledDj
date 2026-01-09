package com.marsraver.wleddj.animations

import android.graphics.Color
import com.marsraver.wleddj.engine.color.ColorUtils
import com.marsraver.wleddj.engine.color.RgbColor
import com.marsraver.wleddj.engine.physics.ParticleSystem
import kotlin.random.Random

/**
 * Popcorn Animation
 * Kernels "pop" upwards and fall back down with gravity.
 */
class PopcornAnimation : BasePixelAnimation() {

    private val particleSystem = ParticleSystem(200)
    private var accumulatedTime = 0.0

    override fun onInit() {
        // Reset state
        particleSystem.clear()
        accumulatedTime = 0.0
        // Clear pixels
        for (i in pixels.indices) pixels[i] = Color.BLACK
    }

    override fun update(now: Long): Boolean {
        fadeToBlackBy(40) // Fade trails

        // Spawn logic
        val spawnRate = (paramIntensity / 255.0) * 0.5 // pops per frame-ish
        accumulatedTime += spawnRate
        
        if (accumulatedTime >= 1.0) {
            val pops = accumulatedTime.toInt()
            accumulatedTime -= pops
            repeat(pops) { spawnPop() }
        } else if (Random.nextDouble() < spawnRate) {
             // Statistical chance for slower rates
             spawnPop()
        }

        // Update physics
        // We iterate manually to apply custom logic if needed, but particleSystem.update() does basic physics.
        // The original code iterates to update AND draw. Let's do that.
        
        val particles = particleSystem.particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            
            p.update()
            
            // Floor bounce check
            if (p.y >= height) {
                // Bounce
                p.y = height - 0.1f
                p.vy *= -0.6f // Dampening
                p.life -= 0.3f // Lose "health" on bounce
            }
            
            // Check death
            p.life -= 0.01f
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }

            // Draw
            if (p.x >= 0 && p.x < width && p.y >= 0 && p.y < height) {
                 // Color depends on velocity
                 // High upward velocity = White/Yellow (Hot)
                 // Falling = Red/Darker (Cooling)
                 
                 val velocityColor = if (p.vy < 0) {
                     // Moving UP (negative Y) -> Hot
                     val color1 = RgbColor.fromInt(p.color)
                     val color2 = RgbColor.WHITE
                     ColorUtils.blend(color1, color2, 128).toInt()
                 } else {
                     // Moving DOWN -> Normal/Cool
                      p.color
                 }
                 
                 // Apply brightness based on life
                 // BasePixelAnimation pixels are ARGB Ints. 
                 // ColorUtils.scaleBrightness expects Int or RgbColor? Let's check ColorUtils.
                 // Assuming RgbColor.scaleBrightness exists? 
                 // The original code used RgbColor objects.
                 // BasePixelAnimation uses Int.
                 
                 // Let's use ColorUtils.scaleBrightness if it supports Int, otherwise helper.
                 // Or manually scale.
                 // Since I don't see ColorUtils source fully, I'll rely on common patterns or simple math.
                 // Actually I saw ColorUtils.kt in the file list but didn't read it.
                 // Let's assume standard int manipulation for now or use the helper in BasePixelAnimation if I added one?
                 // I saw fadeColor in BasePixelAnimation.
                 
                 // Let's implement a simple scale helper locally or use a safe bet.
                 val lifeScale = (p.life * 255).toInt().coerceIn(0, 255)
                 val color = scaleColorBrightness(velocityColor, p.life)
                 
                 setPixelColor(p.x.toInt(), p.y.toInt(), color)
            }
        }
        
        return true
    }
    
    private fun spawnPop() {
        val x = Random.nextFloat() * width
        val y = height - 1.0f // Spawn at bottom
        
        // Initial velocity UP (negative Y)
        val speed = Random.nextFloat() * 1.5f + 1.0f
        val vx = (Random.nextFloat() - 0.5f) * 0.5f // Slight horizontal drift
        
        // Random color
        val color = getColorFromPalette(Random.nextInt(256))
        
        particleSystem.spawn(
            x = x,
            y = y,
            vx = vx,
            vy = -speed,
            ax = 0f,
            ay = 0.05f + (paramSpeed / 255.0f) * 0.05f, // Gravity
            color = color,
            life = 1.0f
        )
    }

    private fun scaleColorBrightness(color: Int, factor: Float): Int {
        val f = factor.coerceIn(0f, 1f)
        val a = Color.alpha(color)
        val r = (Color.red(color) * f).toInt()
        val g = (Color.green(color) * f).toInt()
        val b = (Color.blue(color) * f).toInt()
        return Color.argb(a, r, g, b)
    }
}
