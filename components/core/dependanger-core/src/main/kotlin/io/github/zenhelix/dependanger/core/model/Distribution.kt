package io.github.zenhelix.dependanger.core.model

import io.github.zenhelix.dependanger.core.model.filter.LibraryFilterSpec
import io.github.zenhelix.dependanger.core.model.filter.PluginFilterSpec
import kotlinx.serialization.Serializable

@Serializable
public data class Distribution(
    val name: String,
    val librarySpec: LibraryFilterSpec?,
    val pluginSpec: PluginFilterSpec?,
)
