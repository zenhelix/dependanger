package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.filter.AliasFilter
import io.github.zenhelix.dependanger.core.model.filter.BundleFilter
import io.github.zenhelix.dependanger.core.model.filter.DeprecatedFilter
import io.github.zenhelix.dependanger.core.model.filter.FilterSpec
import io.github.zenhelix.dependanger.core.model.filter.GroupFilter
import io.github.zenhelix.dependanger.core.model.filter.TagExclude
import io.github.zenhelix.dependanger.core.model.filter.TagFilter
import io.github.zenhelix.dependanger.core.model.filter.TagInclude

@DependangerDslMarker
public class FilterDsl {
    public var tagFilter: TagFilter? = null
    public var groupFilter: GroupFilter? = null
    public var aliasFilter: AliasFilter? = null
    public var bundleFilter: BundleFilter? = null
    public var deprecatedFilter: DeprecatedFilter? = null

    public fun filter(block: FilterDsl.() -> Unit) {
        this.apply(block)
    }

    public fun byTags(block: TagFilterDsl.() -> Unit) {
        val dsl = TagFilterDsl().apply(block)
        tagFilter = dsl.toTagFilter()
    }

    public fun byGroups(block: GroupFilterDsl.() -> Unit) {
        val dsl = GroupFilterDsl().apply(block)
        groupFilter = dsl.toGroupFilter()
    }

    public fun byAliases(block: AliasFilterDsl.() -> Unit) {
        val dsl = AliasFilterDsl().apply(block)
        aliasFilter = dsl.toAliasFilter()
    }

    public fun byBundles(block: BundleFilterDsl.() -> Unit) {
        val dsl = BundleFilterDsl().apply(block)
        bundleFilter = dsl.toBundleFilter()
    }

    public fun byDeprecated(block: DeprecatedFilterDsl.() -> Unit) {
        val dsl = DeprecatedFilterDsl().apply(block)
        deprecatedFilter = dsl.toDeprecatedFilter()
    }

    public fun toFilterSpec(): FilterSpec = FilterSpec(
        byTags = tagFilter,
        byGroups = groupFilter,
        byAliases = aliasFilter,
        byBundles = bundleFilter,
        byDeprecated = deprecatedFilter,
        customFilters = emptyMap(),
    )
}

@DependangerDslMarker
public class TagFilterDsl {
    public val includes: MutableList<TagInclude> = mutableListOf()
    public val excludes: MutableList<TagExclude> = mutableListOf()

    public fun include(block: TagIncludeDsl.() -> Unit) {
        val dsl = TagIncludeDsl().apply(block)
        includes.add(TagInclude(anyOf = dsl.anyOfTags.toSet(), allOf = dsl.allOfTags.toSet()))
    }

    public fun exclude(block: TagExcludeDsl.() -> Unit) {
        val dsl = TagExcludeDsl().apply(block)
        excludes.add(TagExclude(anyOf = dsl.anyOfTags.toSet()))
    }

    public fun toTagFilter(): TagFilter = TagFilter(includes = includes.toList(), excludes = excludes.toList())

}

@DependangerDslMarker
public class TagIncludeDsl {
    public val anyOfTags: MutableSet<String> = mutableSetOf()
    public val allOfTags: MutableSet<String> = mutableSetOf()

    public fun anyOf(vararg tags: String) {
        anyOfTags.addAll(tags)
    }

    public fun allOf(vararg tags: String) {
        allOfTags.addAll(tags)
    }
}

@DependangerDslMarker
public class TagExcludeDsl {
    public val anyOfTags: MutableSet<String> = mutableSetOf()

    public fun anyOf(vararg tags: String) {
        anyOfTags.addAll(tags)
    }
}

@DependangerDslMarker
public class GroupFilterDsl {
    public val includes: MutableSet<String> = mutableSetOf()
    public val excludes: MutableSet<String> = mutableSetOf()

    public fun include(block: GroupMatchDsl.() -> Unit) {
        val dsl = GroupMatchDsl().apply(block)
        includes.addAll(dsl.patterns)
    }

    public fun exclude(block: GroupMatchDsl.() -> Unit) {
        val dsl = GroupMatchDsl().apply(block)
        excludes.addAll(dsl.patterns)
    }

    public fun toGroupFilter(): GroupFilter = GroupFilter(includes = includes.toSet(), excludes = excludes.toSet())
}

@DependangerDslMarker
public class GroupMatchDsl {
    public val patterns: MutableList<String> = mutableListOf()

    public fun matching(vararg patterns: String) {
        this.patterns.addAll(patterns)
    }
}

@DependangerDslMarker
public class AliasFilterDsl {
    public val includes: MutableSet<String> = mutableSetOf()
    public val excludes: MutableSet<String> = mutableSetOf()

    public fun include(vararg aliases: String) {
        includes.addAll(aliases)
    }

    public fun exclude(vararg aliases: String) {
        excludes.addAll(aliases)
    }

    public fun toAliasFilter(): AliasFilter = AliasFilter(includes = includes.toSet(), excludes = excludes.toSet())
}

@DependangerDslMarker
public class BundleFilterDsl {
    public val includes: MutableSet<String> = mutableSetOf()
    public val excludes: MutableSet<String> = mutableSetOf()

    public fun include(vararg bundles: String) {
        includes.addAll(bundles)
    }

    public fun exclude(vararg bundles: String) {
        excludes.addAll(bundles)
    }

    public fun toBundleFilter(): BundleFilter = BundleFilter(includes = includes.toSet(), excludes = excludes.toSet())
}

@DependangerDslMarker
public class DeprecatedFilterDsl {
    public var includeDeprecated: Boolean = true

    public fun include() {
        includeDeprecated = true
    }

    public fun exclude() {
        includeDeprecated = false
    }

    public fun toDeprecatedFilter(): DeprecatedFilter = DeprecatedFilter(include = includeDeprecated)
}
