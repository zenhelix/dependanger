package io.github.zenhelix.dependanger.core.model

import io.github.zenhelix.dependanger.core.model.filter.FilterSpec
import kotlinx.serialization.Serializable

@Serializable
public data class Distribution(
    val name: String,
    val spec: FilterSpec? = null,
)
