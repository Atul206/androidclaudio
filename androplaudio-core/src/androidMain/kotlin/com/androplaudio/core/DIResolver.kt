package com.androplaudio.core

import android.app.Application

internal class DIResolver(private val app: Application) : InstanceResolver {

    private val manualInstances = mutableMapOf<String, Any>()

    override fun registerInstance(fqcn: String, instance: Any) {
        manualInstances[fqcn] = instance
    }

    override fun resolve(fqcn: String): Any? {
        manualInstances[fqcn]?.let { return it }
        tryKoin(fqcn)?.let { return it }
        return tryNoArgConstructor(fqcn)
    }

    private fun tryKoin(fqcn: String): Any? {
        return try {
            // GlobalContext is a Kotlin object — must fetch INSTANCE, not invoke with null receiver
            val globalContextClass = Class.forName("org.koin.core.context.GlobalContext")
            val globalContextInstance = globalContextClass.getField("INSTANCE").get(null)
            val koin = globalContextClass.getMethod("get").invoke(globalContextInstance) ?: return null

            val targetKClass = Class.forName(fqcn).kotlin

            // Scan for get(KClass, ...) without hard-coding Qualifier/ParametersHolder types
            val getMethod = koin.javaClass.methods.firstOrNull { m ->
                m.name == "get" &&
                    m.parameterCount >= 1 &&
                    m.parameterTypes[0] == kotlin.reflect.KClass::class.java
            } ?: return null

            val nullArgs = arrayOfNulls<Any>(getMethod.parameterCount - 1)
            getMethod.invoke(koin, targetKClass, *nullArgs)
        } catch (_: Exception) {
            null
        }
    }

    private fun tryNoArgConstructor(fqcn: String): Any? {
        return try {
            Class.forName(fqcn).getDeclaredConstructor().newInstance()
        } catch (_: Exception) {
            null
        }
    }
}
