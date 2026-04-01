package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.filter.AliasFilter
import io.github.zenhelix.dependanger.core.model.filter.BundleFilter
import io.github.zenhelix.dependanger.core.model.filter.DeprecatedFilter
import io.github.zenhelix.dependanger.core.model.filter.GroupFilter
import io.github.zenhelix.dependanger.core.model.filter.LibraryFilterSpec
import io.github.zenhelix.dependanger.core.model.filter.PluginFilterSpec
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

    public fun toLibraryFilterSpec(): LibraryFilterSpec = LibraryFilterSpec(
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
    private val _includes: MutableList<TagInclude> = mutableListOf()
    public val includes: List<TagInclude> get() = _includes.toList()
    private val _excludes: MutableList<TagExclude> = mutableListOf()
    public val excludes: List<TagExclude> get() = _excludes.toList()

    public fun include(block: TagIncludeDsl.() -> Unit) {
        val dsl = TagIncludeDsl().apply(block)
        _includes.add(TagInclude(anyOf = dsl.anyOfTags.toSet(), allOf = dsl.allOfTags.toSet()))
    }

    public fun exclude(block: TagExcludeDsl.() -> Unit) {
        val dsl = TagExcludeDsl().apply(block)
        _excludes.add(TagExclude(anyOf = dsl.anyOfTags.toSet()))
    }

    public fun toTagFilter(): TagFilter = TagFilter(includes = _includes.toList(), excludes = _excludes.toList())

}

@DependangerDslMarker
public class TagIncludeDsl {
    private val _anyOfTags: MutableSet<String> = mutableSetOf()
    public val anyOfTags: Set<String> get() = _anyOfTags.toSet()
    private val _allOfTags: MutableSet<String> = mutableSetOf()
    public val allOfTags: Set<String> get() = _allOfTags.toSet()

    public fun anyOf(vararg tags: String) {
        _anyOfTags.addAll(tags)
    }

    public fun allOf(vararg tags: String) {
        _allOfTags.addAll(tags)
    }
}

@DependangerDslMarker
public class TagExcludeDsl {
    private val _anyOfTags: MutableSet<String> = mutableSetOf()
    public val anyOfTags: Set<String> get() = _anyOfTags.toSet()

    public fun anyOf(vararg tags: String) {
        _anyOfTags.addAll(tags)
    }
}

@DependangerDslMarker
public class GroupFilterDsl {
    private val _includes: MutableSet<String> = mutableSetOf()
    public val includes: Set<String> get() = _includes.toSet()
    private val _excludes: MutableSet<String> = mutableSetOf()
    public val excludes: Set<String> get() = _excludes.toSet()

    public fun include(block: GroupMatchDsl.() -> Unit) {
        val dsl = GroupMatchDsl().apply(block)
        _includes.addAll(dsl.patterns)
    }

    public fun exclude(block: GroupMatchDsl.() -> Unit) {
        val dsl = GroupMatchDsl().apply(block)
        _excludes.addAll(dsl.patterns)
    }

    public fun toGroupFilter(): GroupFilter = GroupFilter(includes = _includes.toSet(), excludes = _excludes.toSet())
}

@DependangerDslMarker
public class GroupMatchDsl {
    private val _patterns: MutableList<String> = mutableListOf()
    public val patterns: List<String> get() = _patterns.toList()

    public fun matching(vararg patterns: String) {
        _patterns.addAll(patterns)
    }
}

@DependangerDslMarker
public class AliasFilterDsl {
    private val _includes: MutableSet<String> = mutableSetOf()
    public val includes: Set<String> get() = _includes.toSet()
    private val _excludes: MutableSet<String> = mutableSetOf()
    public val excludes: Set<String> get() = _excludes.toSet()

    public fun include(vararg aliases: String) {
        _includes.addAll(aliases)
    }

    public fun exclude(vararg aliases: String) {
        _excludes.addAll(aliases)
    }

    public fun toAliasFilter(): AliasFilter = AliasFilter(includes = _includes.toSet(), excludes = _excludes.toSet())
}

@DependangerDslMarker
public class BundleFilterDsl {
    private val _includes: MutableSet<String> = mutableSetOf()
    public val includes: Set<String> get() = _includes.toSet()
    private val _excludes: MutableSet<String> = mutableSetOf()
    public val excludes: Set<String> get() = _excludes.toSet()

    public fun include(vararg bundles: String) {
        _includes.addAll(bundles)
    }

    public fun exclude(vararg bundles: String) {
        _excludes.addAll(bundles)
    }

    public fun toBundleFilter(): BundleFilter = BundleFilter(includes = _includes.toSet(), excludes = _excludes.toSet())
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

@DependangerDslMarker
public class PluginFilterDsl {
    public var tagFilter: TagFilter? = null
    public var aliasFilter: AliasFilter? = null

    public fun byTags(block: TagFilterDsl.() -> Unit) {
        val dsl = TagFilterDsl().apply(block)
        tagFilter = dsl.toTagFilter()
    }

    public fun byAliases(block: AliasFilterDsl.() -> Unit) {
        val dsl = AliasFilterDsl().apply(block)
        aliasFilter = dsl.toAliasFilter()
    }

    public fun toPluginFilterSpec(): PluginFilterSpec = PluginFilterSpec(
        byTags = tagFilter,
        byAliases = aliasFilter,
    )
}
