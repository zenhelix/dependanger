package io.github.zenhelix.dependanger.core.spi

import io.github.zenhelix.dependanger.core.model.Plugin

public interface PluginFilter {
    public val filterId: String
    public fun filter(plugins: List<Plugin>): List<Plugin>
}
