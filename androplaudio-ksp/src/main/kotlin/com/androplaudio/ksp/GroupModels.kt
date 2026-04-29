package com.androplaudio.ksp

import kotlinx.serialization.Serializable

@Serializable
internal data class GroupEntry(
    val id: String,
    val layer: String,
    val `class`: String,
)

@Serializable
internal data class GroupsConfig(
    val version: String,
    val platform: String,
    val framework: String,
    val groups: List<GroupEntry>,
)
