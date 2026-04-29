package com.androplaudio.core

public object AndroClaudio {

    private var server: MCPServer? = null
    private val resolver = IOSResolver()

    public fun initialize(port: Int = 5173, registerGroups: (GroupRegistry) -> Unit = {}) {
        if (server != null) return

        val registry = GroupRegistry()
        registerGroups(registry)

        val modeResolver = ModeResolver()
        val srv = MCPServer(registry, resolver, modeResolver, port)
        srv.start()
        server = srv
    }

    public fun registerInstance(fqcn: String, instance: Any) {
        resolver.registerInstance(fqcn, instance)
    }

    public fun stop() {
        server?.stop()
        server = null
    }
}
