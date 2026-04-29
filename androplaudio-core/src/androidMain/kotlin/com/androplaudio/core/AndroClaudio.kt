package com.androplaudio.core

import android.app.Application
import android.util.Log

public object AndroClaudio {

    private const val TAG = "AndroClaudio"

    private var server: MCPServer? = null
    private var registry: GroupRegistry? = null
    private var diResolver: DIResolver? = null

    @JvmStatic
    public fun initialize(app: Application, port: Int = 5173) {
        if (server != null) return

        val reg = GroupRegistry()
        val di = DIResolver(app)
        val mode = ModeResolver(app)

        try {
            val generatedRegistry = Class.forName("com.androplaudio.generated.GeneratedGroupRegistry")
            val instance = generatedRegistry.getField("INSTANCE").get(null)
            val registerAll = generatedRegistry.getMethod("registerAll", GroupRegistry::class.java)
            registerAll.invoke(instance, reg)
        } catch (_: ClassNotFoundException) {
            Log.w(TAG, "GeneratedGroupRegistry not found — run ./gradlew assembleDebug to trigger KSP")
        }

        registry = reg
        diResolver = di

        val srv = MCPServer(reg, di, mode, port)
        srv.start()
        server = srv

        Log.i(TAG, "MCP server started on port $port — ${reg.size()} groups registered")
    }

    @JvmStatic
    public fun registerInstance(fqcn: String, instance: Any) {
        checkNotNull(diResolver) { "AndroClaudio not initialized — call initialize() first" }
            .registerInstance(fqcn, instance)
    }

    @JvmStatic
    public fun stop() {
        server?.stop()
        server = null
        registry = null
        diResolver = null
    }
}
