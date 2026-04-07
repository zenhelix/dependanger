package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class LibraryFilterSpec(
    val byTags: TagFilter?,
    val byGroups: GroupFilter?,
    val byAliases: AliasFilter?,
    val byBundles: BundleFilter?,
    val byDeprecated: DeprecatedFilter?,
)
