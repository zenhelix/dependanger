package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Bundle

@DependangerDslMarker
public class BundlesDsl {
    public val bundles: MutableList<Bundle> = mutableListOf()

    public fun bundle(name: String, block: BundleDsl.() -> Unit) {
        val dsl = BundleDsl().apply(block)
        bundles.add(Bundle(name = name, libraries = dsl.libraries.toList(), extends = dsl.extendsList.toList()))
    }
}

@DependangerDslMarker
public class BundleDsl {
    public val libraries: MutableList<String> = mutableListOf()
    public val extendsList: MutableList<String> = mutableListOf()

    public fun libraries(vararg aliases: String) {
        libraries.addAll(aliases)
    }

    public fun extends(vararg bundles: String) {
        extendsList.addAll(bundles)
    }
}
