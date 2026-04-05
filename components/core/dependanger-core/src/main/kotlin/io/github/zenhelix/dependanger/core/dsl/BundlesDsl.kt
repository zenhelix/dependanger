package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Bundle

@DependangerDslMarker
public class BundlesDsl {
    private val _bundles: MutableList<Bundle> = mutableListOf()
    public val bundles: List<Bundle> get() = _bundles.toList()

    public fun bundle(name: String, block: BundleDsl.() -> Unit = {}) {
        require(name.isNotBlank()) { "Bundle name must not be blank" }
        require(_bundles.none { it.alias == name }) { "Duplicate bundle name: '$name'" }
        val dsl = BundleDsl().apply(block)
        _bundles.add(Bundle(alias = name, libraries = dsl.libraries.toList(), extends = dsl.extendsList.toList()))
    }
}

@DependangerDslMarker
public class BundleDsl {
    private val _libraries: MutableList<String> = mutableListOf()
    public val libraries: List<String> get() = _libraries.toList()

    private val _extendsList: MutableList<String> = mutableListOf()
    public val extendsList: List<String> get() = _extendsList.toList()

    public fun libraries(vararg aliases: String) {
        _libraries.addAll(aliases)
    }

    public fun extends(vararg bundles: String) {
        _extendsList.addAll(bundles)
    }
}
