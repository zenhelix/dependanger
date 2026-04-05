package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Plugin
import io.github.zenhelix.dependanger.core.model.VersionReference

@DependangerDslMarker
public class PluginsDsl {
    private val _plugins: MutableList<Plugin> = mutableListOf()
    public val plugins: List<Plugin> get() = _plugins.toList()

    public fun plugin(alias: String, id: String) {
        require(alias.isNotBlank()) { "Plugin alias must not be blank" }
        require(_plugins.none { it.alias == alias }) { "Duplicate plugin alias: '$alias'" }
        val (pluginId, version) = parsePluginId(id)
        _plugins.add(Plugin(alias = alias, id = pluginId, version = version, tags = emptySet()))
    }

    public fun plugin(alias: String, id: String, version: VersionReference) {
        require(alias.isNotBlank()) { "Plugin alias must not be blank" }
        require(_plugins.none { it.alias == alias }) { "Duplicate plugin alias: '$alias'" }
        _plugins.add(Plugin(alias = alias, id = id, version = version, tags = emptySet()))
    }

    public fun plugin(alias: String, id: String, block: PluginDsl.() -> Unit) {
        require(alias.isNotBlank()) { "Plugin alias must not be blank" }
        require(_plugins.none { it.alias == alias }) { "Duplicate plugin alias: '$alias'" }
        val (pluginId, version) = parsePluginId(id)
        val dsl = PluginDsl(version).apply(block)
        _plugins.add(Plugin(alias = alias, id = pluginId, version = dsl.version, tags = dsl.tags))
    }

    public fun plugin(alias: String, id: String, version: VersionReference, block: PluginDsl.() -> Unit) {
        require(alias.isNotBlank()) { "Plugin alias must not be blank" }
        require(_plugins.none { it.alias == alias }) { "Duplicate plugin alias: '$alias'" }
        val dsl = PluginDsl(version).apply(block)
        _plugins.add(Plugin(alias = alias, id = id, version = dsl.version, tags = dsl.tags))
    }

    private fun parsePluginId(id: String): Pair<String, VersionReference?> {
        val parts = id.split(":")
        return when (parts.size) {
            1    -> {
                require(parts[0].isNotBlank()) { "Plugin ID must not be blank in '$id'" }
                Pair(parts[0], null)
            }
            2    -> {
                require(parts[0].isNotBlank()) { "Plugin ID must not be blank in '$id'" }
                Pair(parts[0], VersionReference.Literal(parts[1]))
            }
            else -> throw IllegalArgumentException("Invalid plugin id: $id")
        }
    }
}

@DependangerDslMarker
public class PluginDsl(public var version: VersionReference? = null) {
    private val _tags: MutableSet<String> = mutableSetOf()
    public val tags: Set<String> get() = _tags.toSet()

    public fun tags(vararg tags: String) {
        _tags.addAll(tags)
    }
}
