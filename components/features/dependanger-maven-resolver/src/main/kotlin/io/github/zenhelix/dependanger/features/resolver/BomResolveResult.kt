package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.core.model.Diagnostics

internal data class BomResolveResult(
    val content: BomContent,
    val diagnostics: Diagnostics,
)
