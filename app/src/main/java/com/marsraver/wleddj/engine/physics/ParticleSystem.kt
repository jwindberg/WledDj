package com.marsraver.wleddj.engine.physics

import android.graphics.Color

class ParticleSystem(val maxParticles: Int = 100) {
    val particles = ArrayList<Particle>(maxParticles)

    fun update() {
        // Use iterator to safely remove dead particles while iterating
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.update()
            if (p.isDead()) {
                iterator.remove()
            }
        }
    }

    fun spawn(
        x: Float, y: Float,
        vx: Float = 0f, vy: Float = 0f,
        ax: Float = 0f, ay: Float = 0f,
        color: Int = Color.WHITE,
        life: Float = 1.0f
    ): Particle? {
        if (particles.size >= maxParticles) return null
        
        val p = Particle(
            x = x, y = y,
            vx = vx, vy = vy,
            ax = ax, ay = ay,
            color = color,
            life = life
        )
        particles.add(p)
        return p
    }

    fun clear() {
        particles.clear()
    }
}
