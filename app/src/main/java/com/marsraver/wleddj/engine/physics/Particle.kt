package com.marsraver.wleddj.engine.physics

import android.graphics.Color

/**
 * A generic particle for use in animations.
 */
data class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var ax: Float = 0f,
    var ay: Float = 0f,
    var color: Int = Color.BLACK,
    var age: Float = 0f,      // How long it has been alive
    var life: Float = 1.0f,   // Max life or current health (1.0 -> 0.0)
    var size: Float = 1.0f,
    
    // Custom data slots to avoid extending for simple cases
    var data1: Float = 0f, 
    var data2: Float = 0f,
    var data3: Int = 0,
    var sourceIndex: Int = 0 // e.g. which LED it spawned from or color palette index
) {
    fun update() {
        vx += ax
        vy += ay
        x += vx
        y += vy
        age += 1f // Frame count or time... let's assume frames usually
    }

    fun isDead(): Boolean {
        // Simple life check. Animations can use age vs maxLife or health approach.
        return life <= 0f
    }
}
