package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.Severity
import kotlinx.serialization.Serializable

@Serializable
public data class ProcessingDiagnostics(
    val errors: List<DiagnosticMessage> = emptyList(),
    val warnings: List<DiagnosticMessage> = emptyList(),
    val infos: List<DiagnosticMessage> = emptyList(),
) {
    public val hasErrors: Boolean get() = errors.isNotEmpty()
}

@Serializable
public data class DiagnosticMessage(
    val code: String,
    val message: String,
    val severity: Severity,
    val processorId: String? = null,
)
