package com.androplaudio.core

internal class IOSResolver : InstanceResolver {

    private val instances = mutableMapOf<String, Any>()

    override fun registerInstance(fqcn: String, instance: Any) {
        instances[fqcn] = instance
    }

    override fun resolve(fqcn: String): Any? = instances[fqcn]
}
