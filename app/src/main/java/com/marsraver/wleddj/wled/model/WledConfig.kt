package com.marsraver.wleddj.wled.model

import kotlinx.serialization.Serializable

@Serializable
data class WledConfigResponse(
    val hw: WledHwConfig? = null
)

@Serializable
data class WledHwConfig(
    val led: WledLedConfig? = null
)

@Serializable
data class WledLedConfig(
    val total: Int = 0,
    val matrix: WledMatrixConfig? = null
)

@Serializable
data class WledMatrixConfig(
    val mpc: Int = 0,
    val panels: List<WledPanelConfig>? = null
)

@Serializable
data class WledPanelConfig(
    val w: Int = 0,
    val h: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val b: Boolean = false, // Bottom start
    val r: Boolean = false, // Right start
    val v: Boolean = false, // Vertical
    val s: Boolean = false  // Serpentine
)
