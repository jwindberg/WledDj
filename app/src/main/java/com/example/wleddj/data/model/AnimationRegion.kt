package com.example.wleddj.data.model

import android.graphics.RectF
import com.example.wleddj.engine.Animation
import java.util.UUID

data class AnimationRegion(
    val id: String = UUID.randomUUID().toString(),
    var rect: RectF, // Virtual Canvas Coordinates
    var rotation: Float = 0f, // Degrees
    val animation: Animation
)
