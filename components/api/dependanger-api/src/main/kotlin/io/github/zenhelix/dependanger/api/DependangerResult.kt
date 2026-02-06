package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.effective.model.DiagnosticMessage
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ProcessingDiagnostics

public sealed class DependangerResult {
    public abstract val diagnostics: ProcessingDiagnostics

    public data class Success(
        val effective: EffectiveMetadata,
        override val diagnostics: ProcessingDiagnostics,
    ) : DependangerResult()

    public data class Failure(
        val errors: List<DiagnosticMessage>,
        override val diagnostics: ProcessingDiagnostics,
    ) : DependangerResult()
}
