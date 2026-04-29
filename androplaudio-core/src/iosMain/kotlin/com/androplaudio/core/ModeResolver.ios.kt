package com.androplaudio.core

import kotlinx.serialization.json.Json
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

internal actual class ModeResolver {

    private val config: AndroplaudoConfig by lazy { loadConfig() }

    private fun loadConfig(): AndroplaudoConfig {
        return try {
            val path = NSBundle.mainBundle.pathForResource(
                "androplaudio.config",
                ofType = "json",
            ) ?: return AndroplaudoConfig()
            @Suppress("CAST_NEVER_SUCCEEDS")
            val text = NSString.stringWithContentsOfFile(
                path,
                encoding = NSUTF8StringEncoding,
                error = null,
            ) as? String ?: return AndroplaudoConfig()
            Json { ignoreUnknownKeys = true }.decodeFromString(text)
        } catch (_: Exception) {
            AndroplaudoConfig()
        }
    }

    actual fun isLive(groupId: String): Boolean {
        val groupMode = config.groups[groupId] ?: config.defaultMode
        return groupMode == "live"
    }
}
