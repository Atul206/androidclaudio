package com.androplaudio.core

internal expect class MCPServer(
    registry: GroupRegistry,
    diResolver: InstanceResolver?,
    modeResolver: ModeResolver,
    port: Int,
) {
    fun start()
    fun stop()
}
