package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class FilterSpec(
    val byTags: TagFilter? = null,
    val byGroups: GroupFilter? = null,
    val byAliases: AliasFilter? = null,
    val byBundles: BundleFilter? = null,
    val byDeprecated: DeprecatedFilter? = null,
    val customFilters: Map<String, JsonElement> = emptyMap(),
)
