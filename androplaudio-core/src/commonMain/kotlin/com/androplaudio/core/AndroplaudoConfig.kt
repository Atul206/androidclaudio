package com.androplaudio.core

import kotlinx.serialization.Serializable

@Serializable
internal data class AndroplaudoConfig(
    val defaultMode: String = "mock",
    val groups: Map<String, String> = emptyMap(),
)
