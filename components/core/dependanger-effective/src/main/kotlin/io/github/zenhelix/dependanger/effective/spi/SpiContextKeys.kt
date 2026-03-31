package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey

public val LibraryFiltersKey: ProcessingContextKey<List<LibraryFilter>> =
    ProcessingContextKey("libraryFilters")

public val PluginFiltersKey: ProcessingContextKey<List<PluginFilter>> =
    ProcessingContextKey("pluginFilters")

public val CustomRuleHandlersKey: ProcessingContextKey<Map<String, CustomRuleHandler>> =
    ProcessingContextKey("customRuleHandlers")
