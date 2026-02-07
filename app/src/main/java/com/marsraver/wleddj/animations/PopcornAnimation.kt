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

    private val particleSystem = ParticleSystem(1500) // Much more corn!
    private var accumulatedTime = 0.0
    private var lastTimeNs = 0L
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    override fun supportsSpeed(): Boolean = true

    override fun onInit() {
        particleSystem.clear()
        accumulatedTime = 0.0
        lastTimeNs = System.nanoTime()
    }
    
    override fun update(now: Long): Boolean {
        // Driven by draw()
        return true 
    }

    override fun draw(canvas: android.graphics.Canvas, width: Float, height: Float) {
        // Update dimensions
        this.width = width.toInt()
        this.height = height.toInt()
        
        // Initial Pre-fill (if empty or resized significantly?)
        // If particle count is 0, we definitely need to fill.
        // But onInit calls clear().
        if (particleSystem.particles.isEmpty()) {
            preFillBottomLayer()
        }
        
        // Time Delta
        val now = System.nanoTime()
        if (lastTimeNs == 0L) lastTimeNs = now
        val dt = (now - lastTimeNs) / 1_000_000_000.0 
        lastTimeNs = now
        
        val timeStep = (dt * 60.0).toFloat().coerceIn(0.1f, 3.0f)
        
        simulate(timeStep)
        
        // Render
        canvas.drawColor(android.graphics.Color.BLACK)
        paint.style = android.graphics.Paint.Style.FILL
        
        val radius = (width * 0.012f).coerceAtLeast(3.0f)
        
        // Draw Particles (Reverse order so new ones are on top? Or standard?)
        // Standard is fine.
        val particles = particleSystem.particles
        for (p in particles) {
             val color: Int
             if (p.data3 == 1) { // Landed
                 color = p.color 
             } else {
                 val velocityColor = if (p.vy < 0) {
                     val color1 = RgbColor.fromInt(p.color)
                     val color2 = RgbColor.WHITE
                     ColorUtils.blend(color1, color2, 128).toInt()
                 } else {
                      p.color
                 }
                 color = scaleColorBrightness(velocityColor, p.life)
             }
             paint.color = color
             
             // Draw Lumpy Popcorn with Rotation
             // data1 = Rotation Angle (degrees)
             canvas.save()
             canvas.rotate(p.data1, p.x, p.y)
             
             // Main body
             canvas.drawCircle(p.x, p.y, radius, paint)
             // Lumps (using fixed offsets relative to radius, rotated by canvas)
             // To make them look unique, we could use p.hashCode/random, 
             // but consistent "Popcorn" shape is fine.
             // Let's add 2 lumps.
             canvas.drawCircle(p.x - radius*0.6f, p.y - radius*0.6f, radius*0.6f, paint)
             canvas.drawCircle(p.x + radius*0.5f, p.y - radius*0.7f, radius*0.5f, paint)
             
             canvas.restore()
        }
    }
    
    private fun simulate(timeStep: Float) {
        // 1. Build "Terrain"
        val radius = (width * 0.012f).coerceAtLeast(3.0f)
        val diameter = radius * 2.1f
        val numBuckets = (width / diameter).toInt().coerceAtLeast(1)
        val bucketHeights = FloatArray(numBuckets) { 0f }
        val bucketCounts = IntArray(numBuckets) { 0 }
        
        val particles = particleSystem.particles
        
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            
            if (p.data3 == 1) { // Landed
                val col = (p.x / diameter).toInt().coerceIn(0, numBuckets - 1)
                
                // Avalanche Logic: Check neighbors for lower ground
                var targetCol = col
                var bestHeight = bucketCounts[col]
                
                // Check Left
                if (col > 0 && bucketCounts[col - 1] < bestHeight - 1) {
                    targetCol = col - 1
                    bestHeight = bucketCounts[col - 1]
                }
                // Check Right
                if (col < numBuckets - 1 && bucketCounts[col + 1] < bestHeight - 1) { 
                    // If left was also a hole, pick random
                    if (targetCol != col) {
                         if (Random.nextBoolean()) targetCol = col + 1
                    } else {
                         targetCol = col + 1
                    }
                }
                
                // Roll towards hole
                if (targetCol != col) {
                    val targetX = targetCol * diameter + radius
                    val moveSpeed = diameter * 1.5f * timeStep 
                    
                    val dx: Float
                    if (p.x < targetX) {
                        dx = moveSpeed
                        p.x += dx
                        if (p.x > targetX) p.x = targetX
                    } else {
                        dx = -moveSpeed
                        p.x += dx
                        if (p.x < targetX) p.x = targetX
                    }
                    
                    // Tumble while rolling!
                    // angle += distance * constant
                    p.data1 += dx * 5.0f // Spin based on movement
                }
                
                // Re-calculate column based on where particle Is
                val finalCol = (p.x / diameter).toInt().coerceIn(0, numBuckets - 1)
                
                // Fall Logic
                val targetY = height - radius - (bucketCounts[finalCol] * diameter)
                val fallSpeed = 5.0f * timeStep
                
                if (p.y < targetY) { // Falling down
                     p.y += fallSpeed
                     if (p.y > targetY) p.y = targetY
                } else if (p.y > targetY) { // Popping up (buried)
                     p.y = targetY 
                }
                
                // Register in new column
                bucketCounts[finalCol]++
                bucketHeights[finalCol] = (bucketCounts[finalCol] * diameter)
                
                p.vx = 0f
                p.vy = 0f
                // Damping spin when settled
                p.data2 *= 0.8f
                p.data1 += p.data2 * timeStep
            }
        }

        // 2. Spawn / Heat Logic
        // User requested "Much Slower" at low end.
        // Curve: Exponential
        // Min: 0.0005 (1 pop every ~33 seconds at 60fps) -> Very slow!
        // Max: 0.8 (Machine gun)
        val speedNorm = paramSpeed / 255.0
        val heat = 0.0005 + (speedNorm * speedNorm * speedNorm) * 0.8
        
        accumulatedTime += heat * timeStep
        if (accumulatedTime >= 1.0) {
            val pops = accumulatedTime.toInt()
            accumulatedTime -= pops
            repeat(pops) { spawnPop() }
        } else if (Random.nextDouble() < (heat * timeStep)) {
             spawnPop()
        }

        // 3. Update Flying Particles
        val flyingIterator = particles.iterator()
        while (flyingIterator.hasNext()) {
            // ... (rest of logic same) ...
            val p = flyingIterator.next()
            if (p.data3 == 1) continue 
            
            p.vy += p.ay * timeStep 
            p.vx += p.ax * timeStep
            p.x += p.vx * timeStep
            p.y += p.vy * timeStep
            
            // Tumble in air (Spin)
            p.data1 += p.data2 * timeStep
            
            if (p.x < 0) {
                p.x = 0.1f
                p.vx *= -0.8f
                p.data2 *= -0.5f // Reverse spin on wall hit
            } else if (p.x > width) {
                p.x = width - 0.1f
                p.vx *= -0.8f
                p.data2 *= -0.5f
            }
            
            if (p.y < 0) {
                p.y = 0.1f
                p.vy *= -0.7f 
                p.data2 += (Random.nextFloat() - 0.5f) * 20.0f // Chaos spin on lid hit
            }
            
            val b = (p.x / diameter).toInt().coerceIn(0, numBuckets - 1)
            val stackHeight = bucketHeights[b]
            val floorY = height - stackHeight - radius
            
            if (p.vy > 0 && p.y >= floorY) {
                p.y = floorY
                p.data3 = 1 
                p.vx = 0f
                p.vy = 0f
                p.life = 1.0f 
                p.data2 = 0f // Stop spinning (mostly)
                bucketHeights[b] += diameter
            }
            
            p.life -= 0.01f * timeStep
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }
        }
    }
    
    // ... spawnPop same ...
    
    private fun preFillBottomLayer() {
        if (width <= 0 || height <= 0) return
        
        val radius = (width * 0.012f).coerceAtLeast(3.0f)
        val diameter = radius * 2.1f
        
        val numCols = (width / diameter).toInt()
        // 1/8th of screen height
        val numRows = (height * 0.125f / diameter).toInt().coerceAtLeast(2)
        
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                if (particleSystem.particles.size >= particleSystem.maxParticles) return
                
                // Add jitter to X so they don't look perfectly grid-like
                val x = col * diameter + radius + ((Random.nextFloat() - 0.5f) * radius * 0.5f)
                val y = height - radius - (row * diameter) // Stack them roughly
                
                val color = getColorFromPalette(Random.nextInt(256))
                
                particleSystem.spawn(
                    x = x,
                    y = y,
                    vx = 0f, vy = 0f,
                    ax = 0f, ay = 0f,
                    color = color,
                    life = 1.0f,
                    data1 = Random.nextFloat() * 360f, // Random Angle
                    data2 = 0f, // No spin
                    data3 = 1 // Landed
                )
            }
        }
    }
    
    private fun spawnPop() {
        // Ignite!
        // Pick an X location
        val x = Random.nextFloat() * width
        
        // Agitate: Find particles near X and Blast them
        val radius = (width * 0.012f).coerceAtLeast(10.0f) // Blast radius larger than corn
        val blastRadius = radius * 8.0f 
        
        // Iterate all particles to find who to blast?
        // With 1500 particles, O(N) is cheap enough (1500 checks is nothing).
        for (p in particleSystem.particles) {
            if (p.data3 == 1) { // Only blast Landed ones
                val dx = p.x - x
                // Simple X-distance check for speed, ignoring Y (blast entire column)
                // Or proper distance?
                // Visuals: Blast column is better for "pops go off *below* them"
                
                if (kotlin.math.abs(dx) < blastRadius) {
                    // It's in the shockwave!
                    // Launch it!
                    p.data3 = 0 // Flying
                    
                    // Upward velocity + random spread
                    val intensity = paramIntensity / 255f
                    val blastPower = 15.0f + (intensity * 20.0f) // Very strong
                    
                    // Randomize slightly so they don't move in unison
                    val rnd = Random.nextFloat()
                    p.vy = -(blastPower * (0.5f + rnd))
                    
                    // Horizontal scatter
                    p.vx = dx * 0.1f + (Random.nextFloat() - 0.5f) * 5.0f
                    
                    p.ay = height * 0.002f // Reset gravity just in case
                    
                    // Add Spin on agitation!
                    p.data2 = (Random.nextFloat() - 0.5f) * 40.0f 
                }
            }
        }
        
        // Also spawn a NEW particle to add to the chaos/volume
        if (particleSystem.particles.size < particleSystem.maxParticles) {
            val y = height - 1.0f
             
            val gravity = height * 0.002f
            // High velocity launch
            val intensityFactor = paramIntensity / 255f
            val baseMax = 0.8f + (intensityFactor * 1.5f)
            val targetHeightPct = 0.4f + Random.nextFloat() * (baseMax - 0.4f)
            val targetDist = height * targetHeightPct
            val speed = kotlin.math.sqrt(2.0 * gravity * targetDist).toFloat()
            
            // Angle
            val angle = (Random.nextFloat() - 0.5f) * 1.5f
            val vx = (speed * kotlin.math.tan(angle)).toFloat() * 0.5f
            
            val color = getColorFromPalette(Random.nextInt(256))
            
            particleSystem.spawn(
                x = x,
                y = y,
                vx = vx,
                vy = -speed,
                ax = 0f,
                ay = gravity, 
                color = color,
                life = 1.0f,
                data1 = Random.nextFloat() * 360f, // Angle
                data2 = (Random.nextFloat() - 0.5f) * 60.0f, // Spin Velocity
                data3 = 0 // Flying
            )
        }
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
