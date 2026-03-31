package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import java.util.ServiceLoader

public class CoreSpiContextContributor : ContextContributor {
    private val libraryFilters = ServiceLoader.load(LibraryFilter::class.java).toList()
    private val pluginFilters = ServiceLoader.load(PluginFilter::class.java).toList()
    private val ruleHandlers = ServiceLoader.load(CustomRuleHandler::class.java).associateBy { it.ruleType }

    override fun contribute(): Map<ProcessingContextKey<*>, Any> = mapOf(
        LibraryFiltersKey to libraryFilters,
        PluginFiltersKey to pluginFilters,
        CustomRuleHandlersKey to ruleHandlers,
    )
}
