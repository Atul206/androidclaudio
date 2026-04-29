package com.androplaudio.ksp

internal object LifecycleFilter {

    private val EXCLUDED_METHODS = setOf(
        // Android lifecycle
        "onCreate", "onDestroy", "onStart", "onStop",
        "onPause", "onResume", "onBind", "onUnbind",
        "onStartCommand", "onRebind", "onReceive",
        "doWork", "onStopped", "getForegroundInfo",
        // Kotlin/Java object methods
        "equals", "hashCode", "toString", "copy",
        // Constructors (KSP surfaces these as functions)
        "<init>", "<clinit>",
        // ContentProvider / database
        "query", "insert", "delete", "update", "getType", "openFile",
    )

    private val EXCLUDED_PREFIXES = listOf(
        "component",  // Kotlin data class componentN()
        "access\$",   // Kotlin synthetic accessors
    )

    internal fun shouldExclude(methodName: String): Boolean {
        if (methodName in EXCLUDED_METHODS) return true
        return EXCLUDED_PREFIXES.any { methodName.startsWith(it) }
    }
}
