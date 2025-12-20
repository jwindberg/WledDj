package com.example.wleddj.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Installation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val width: Float = 1000f, // Virtual units
    val height: Float = 1000f,
    val devices: List<WledDevice> = emptyList()
)
