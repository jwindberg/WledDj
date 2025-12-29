package com.marsraver.wleddj.model

import kotlinx.serialization.Serializable

@Serializable
data class WledDevice(
    val ip: String,
    val macAddress: String,
    val name: String,
    val pixelCount: Int,
    // Position/Size in Installation virtual units
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    // Rotation in degrees
    val rotation: Float = 0f,
    // For Matrices: number of LEDs in a row. 0 or 1 implies linear strip.
    val segmentWidth: Int = 0,
    // Matrix Metadata
    val is2D: Boolean = false,
    val matrixWidth: Int = 0,
    val matrixHeight: Int = 0,
    val serpentine: Boolean = false,
    // Human readable descriptions
    val firstLed: String = "",
    val orientation: String = "",
    val panelDescription: String = ""
)
