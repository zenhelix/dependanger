package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public data class DependangerResult(
    val effective: EffectiveMetadata?,
    val diagnostics: Diagnostics,
) {
    public val isSuccess: Boolean get() = effective != null && !diagnostics.hasErrors
}
