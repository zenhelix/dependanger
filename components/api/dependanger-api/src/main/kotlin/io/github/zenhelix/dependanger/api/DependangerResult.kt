package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ProcessingDiagnostics

public sealed class DependangerResult {
    public data class Success(
        val effective: EffectiveMetadata,
        val diagnostics: ProcessingDiagnostics,
    ) : DependangerResult()

    public data class Failure(
        val errors: List<String>,
        val diagnostics: ProcessingDiagnostics,
    ) : DependangerResult()
}
