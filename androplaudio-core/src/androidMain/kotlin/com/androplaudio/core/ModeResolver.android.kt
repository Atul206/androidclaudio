package com.androplaudio.core

import android.content.Context
import kotlinx.serialization.json.Json

internal actual class ModeResolver(private val context: Context) {

    private val config: AndroplaudoConfig by lazy { loadConfig() }

    private fun loadConfig(): AndroplaudoConfig {
        return try {
            val stream = context.assets.open("androplaudio.config.json")
            Json { ignoreUnknownKeys = true }.decodeFromString(stream.bufferedReader().readText())
        } catch (_: Exception) {
            AndroplaudoConfig()
        }
    }

    actual fun isLive(groupId: String): Boolean {
        val groupMode = config.groups[groupId] ?: config.defaultMode
        return groupMode == "live"
    }
}
