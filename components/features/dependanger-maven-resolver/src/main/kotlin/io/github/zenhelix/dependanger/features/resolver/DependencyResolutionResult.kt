package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.core.model.Diagnostics

internal data class DependencyResolutionResult(
    val dependency: BomDependency?,
    val diagnostics: Diagnostics,
)
