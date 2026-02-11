package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class FilterSpec(
    val byTags: TagFilter?,
    val byGroups: GroupFilter?,
    val byAliases: AliasFilter?,
    val byBundles: BundleFilter?,
    val byDeprecated: DeprecatedFilter?,
    val customFilters: Map<String, JsonElement>,
)
