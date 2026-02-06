package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Plugin
import io.github.zenhelix.dependanger.core.model.VersionReference

@DependangerDslMarker
public class PluginsDsl {
    public val plugins: MutableList<Plugin> = mutableListOf()

    public fun plugin(alias: String, id: String) {
        val (pluginId, version) = parsePluginId(id)
        plugins.add(Plugin(alias = alias, id = pluginId, version = version))
    }

    public fun plugin(alias: String, id: String, version: VersionReference) {
        plugins.add(Plugin(alias = alias, id = id, version = version))
    }

    public fun plugin(alias: String, id: String, block: PluginDsl.() -> Unit) {
        val (pluginId, version) = parsePluginId(id)
        val dsl = PluginDsl(version).apply(block)
        plugins.add(Plugin(alias = alias, id = pluginId, version = dsl.version, tags = dsl.tags.toSet()))
    }

    public fun plugin(alias: String, id: String, version: VersionReference, block: PluginDsl.() -> Unit) {
        val dsl = PluginDsl(version).apply(block)
        plugins.add(Plugin(alias = alias, id = id, version = dsl.version, tags = dsl.tags.toSet()))
    }

    private fun parsePluginId(id: String): Pair<String, VersionReference?> {
        val parts = id.split(":")
        return when (parts.size) {
            1    -> Pair(parts[0], null)
            2    -> Pair(parts[0], VersionReference.Literal(parts[1]))
            else -> throw IllegalArgumentException("Invalid plugin id: $id")
        }
    }
}

@DependangerDslMarker
public class PluginDsl(public var version: VersionReference? = null) {
    public val tags: MutableSet<String> = mutableSetOf()

    public fun tags(vararg tags: String) {
        this.tags.addAll(tags)
    }
}
