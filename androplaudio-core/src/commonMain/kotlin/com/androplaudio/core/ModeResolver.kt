package com.androplaudio.core

internal expect class ModeResolver {
    fun isLive(groupId: String): Boolean
}
