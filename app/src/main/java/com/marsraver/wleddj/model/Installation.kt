package com.marsraver.wleddj.model

import kotlinx.serialization.Serializable
import java.util.UUID
import com.marsraver.wleddj.engine.color.Palette

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
    val cameraZoom: Float = 1f,  // Multiplier of Base Fit Scale
    val animations: List<SavedAnimation> = emptyList()
)

@Serializable
data class SavedAnimation(
    val id: String,
    val type: AnimationType,
    val rectLeft: Float, 
    val rectTop: Float, 
    val rectRight: Float, 
    val rectBottom: Float,
    val rotation: Float = 0f,
    val text: String? = null,
    val primaryColor: Int? = null,
    val secondaryColor: Int? = null,
    val paletteName: Palette? = null
)
