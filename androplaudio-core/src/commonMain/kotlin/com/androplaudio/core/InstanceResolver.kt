package com.androplaudio.core

internal interface InstanceResolver {
    fun resolve(fqcn: String): Any?
    fun registerInstance(fqcn: String, instance: Any)
}
