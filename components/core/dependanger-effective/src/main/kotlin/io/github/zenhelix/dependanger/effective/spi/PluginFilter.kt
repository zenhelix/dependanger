package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.effective.model.EffectivePlugin
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext

public interface PluginFilter {
    public val filterId: String
    public fun shouldInclude(alias: String, plugin: EffectivePlugin, context: ProcessingContext): Boolean
}
