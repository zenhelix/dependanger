package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.effective.model.EffectiveBundle
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectivePlugin

public data class FilterPreview(
    val distribution: String,
    val included: FilteredItems,
    val excluded: FilteredItems,
)

public data class FilteredItems(
    val libraries: Map<String, EffectiveLibrary>,
    val plugins: Map<String, EffectivePlugin>,
    val bundles: Map<String, EffectiveBundle>,
)
