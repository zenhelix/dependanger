package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Distribution
import io.github.zenhelix.dependanger.core.model.filter.FilterSpec

@DependangerDslMarker
public class DistributionsDsl {
    public val distributions: MutableList<Distribution> = mutableListOf()

    public fun distribution(name: String, block: DistributionDsl.() -> Unit = {}) {
        val dsl = DistributionDsl().apply(block)
        distributions.add(Distribution(name = name, spec = dsl.filterSpec))
    }
}

@DependangerDslMarker
public class DistributionDsl {
    public var filterSpec: FilterSpec? = null

    public fun spec(block: FilterDsl.() -> Unit) {
        val dsl = FilterDsl().apply(block)
        filterSpec = dsl.toFilterSpec()
    }
}
