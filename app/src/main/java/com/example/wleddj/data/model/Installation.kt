package com.example.wleddj.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Installation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val width: Float = 1000f, // Virtual units
    val height: Float = 1000f,
    val devices: List<WledDevice> = emptyList(),
    val viewportZoom: Float = 1f, // Kept for backward compat if needed, but we will move to camera*
    val viewportPanX: Float = 0f,
    val viewportPanY: Float = 0f,
    val cameraX: Float? = null, // Virtual Center X
    val cameraY: Float? = null, // Virtual Center Y
    val cameraZoom: Float = 1f  // Multiplier of Base Fit Scale
)
