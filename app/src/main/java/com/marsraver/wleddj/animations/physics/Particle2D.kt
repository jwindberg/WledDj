package com.marsraver.wleddj.animations.physics

import android.graphics.Color

data class Particle2D(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var mass: Float = 1f,
    var lifetime: Float = 1f, // 1.0 down to 0.0
    var decayRate: Float = 0.01f,
    var color: Int = Color.WHITE,
    var active: Boolean = true,
    var bounceX: Boolean = true,
    var bounceY: Boolean = true,
    var groundCollision: Boolean = true,
    var type: Int = 0,
    var size: Int = 3
)
