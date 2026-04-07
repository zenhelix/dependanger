package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class AliasFilter(
    override val includes: Set<String>,
    override val excludes: Set<String>,
) : IncludeExcludeFilter
