package com.marsraver.wleddj.wled.model

import kotlinx.serialization.Serializable

@Serializable
data class WledInfoResponse(
    val leds: WledLedsInfo,
    val name: String
)

@Serializable
data class WledLedsInfo(
    val count: Int,
    val w: Int = 0,
    val h: Int = 0
)
