package io.github.zenhelix.dependanger.effective.spi

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import java.util.ServiceLoader

private val logger = KotlinLogging.logger {}

public class CoreSpiContextContributor : ContextContributor {
    private val libraryFilters = ServiceLoader.load(LibraryFilter::class.java).toList()
    private val pluginFilters = ServiceLoader.load(PluginFilter::class.java).toList()
    private val ruleHandlers: Map<String, CustomRuleHandler> = run {
        val handlers = ServiceLoader.load(CustomRuleHandler::class.java).toList()
        val grouped = handlers.groupBy { it.ruleType }
        grouped.forEach { (ruleType, group) ->
            if (group.size > 1) {
                logger.warn { "Duplicate CustomRuleHandler for ruleType: $ruleType (${group.size} implementations), using last registered" }
            }
        }
        handlers.associateBy { it.ruleType }
    }

    override fun contribute(): Map<ProcessingContextKey<*>, Any> = mapOf(
        LibraryFiltersKey to libraryFilters,
        PluginFiltersKey to pluginFilters,
        CustomRuleHandlersKey to ruleHandlers,
    )
}
