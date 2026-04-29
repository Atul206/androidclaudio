package com.androplaudio.core

internal typealias CallHandler = (fn: String, args: Map<String, Any?>, instance: Any) -> Any?

internal data class RegisteredGroup(
    val id: String,
    val layer: String,
    val className: String,
    val tools: List<ToolMetadata>,
    val callHandler: CallHandler,
)

public class GroupRegistry {

    private val groups = mutableMapOf<String, RegisteredGroup>()

    public fun register(
        id: String,
        layer: String,
        className: String,
        tools: List<ToolMetadata>,
        callHandler: CallHandler,
    ) {
        groups[id] = RegisteredGroup(id, layer, className, tools, callHandler)
    }

    internal fun allDescriptors(): List<GroupDescriptor> =
        groups.values.map { GroupDescriptor(it.id, it.layer, it.className, it.tools.size) }

    internal fun getDetail(groupId: String): GroupDetail? =
        groups[groupId]?.let { GroupDetail(it.id, it.layer, it.className, it.tools) }

    internal fun getGroup(groupId: String): RegisteredGroup? = groups[groupId]

    internal fun size(): Int = groups.size
}
