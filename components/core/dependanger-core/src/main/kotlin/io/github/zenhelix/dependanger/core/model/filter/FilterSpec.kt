package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class FilterSpec(
    val byTags: TagFilter? = null,
    val byGroups: GroupFilter? = null,
    val byAliases: AliasFilter? = null,
    val byBundles: BundleFilter? = null,
    val byDeprecated: DeprecatedFilter? = null,
)
