package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Distribution
import io.github.zenhelix.dependanger.core.model.filter.LibraryFilterSpec
import io.github.zenhelix.dependanger.core.model.filter.PluginFilterSpec

@DependangerDslMarker
public class DistributionsDsl {
    private val _distributions: MutableList<Distribution> = mutableListOf()
    public val distributions: List<Distribution> get() = _distributions.toList()

    public fun distribution(name: String, block: DistributionDsl.() -> Unit = {}) {
        val dsl = DistributionDsl().apply(block)
        _distributions.add(Distribution(name = name, librarySpec = dsl.libraryFilterSpec, pluginSpec = dsl.pluginFilterSpec))
    }
}

@DependangerDslMarker
public class DistributionDsl {
    public var libraryFilterSpec: LibraryFilterSpec? = null
    public var pluginFilterSpec: PluginFilterSpec? = null

    public fun spec(block: FilterDsl.() -> Unit) {
        val dsl = FilterDsl().apply(block)
        libraryFilterSpec = dsl.toLibraryFilterSpec()
    }

    public fun pluginSpec(block: PluginFilterDsl.() -> Unit) {
        val dsl = PluginFilterDsl().apply(block)
        pluginFilterSpec = dsl.toPluginFilterSpec()
    }
}
