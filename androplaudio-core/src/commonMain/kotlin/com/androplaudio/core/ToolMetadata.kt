package com.androplaudio.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class ParamMetadata(
    val name: String,
    val type: String,
)

@Serializable
public data class ToolMetadata(
    val name: String,
    val params: List<ParamMetadata>,
    val returnType: String,
)

@Serializable
internal data class GroupDescriptor(
    val id: String,
    val layer: String,
    val className: String,
    val toolCount: Int,
)

@Serializable
internal data class GroupDetail(
    val id: String,
    val layer: String,
    val className: String,
    val tools: List<ToolMetadata>,
)

@Serializable
internal data class CallRequest(
    val group: String,
    val fn: String,
    val args: Map<String, JsonElement> = emptyMap(),
)

@Serializable
internal data class CallResponse(
    val result: JsonElement? = null,
    val error: String? = null,
    val mock: Boolean = false,
)
