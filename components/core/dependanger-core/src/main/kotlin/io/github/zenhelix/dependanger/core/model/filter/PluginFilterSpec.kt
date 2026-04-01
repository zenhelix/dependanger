package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class PluginFilterSpec(
    val byTags: TagFilter?,
    val byAliases: AliasFilter?,
)
