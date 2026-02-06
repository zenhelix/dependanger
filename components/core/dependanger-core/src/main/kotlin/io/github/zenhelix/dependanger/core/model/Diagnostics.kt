package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class DiagnosticMessage(
    val code: String,
    val message: String,
    val severity: Severity,
    val source: String? = null,
)

@Serializable
public data class Diagnostics(
    val errors: List<DiagnosticMessage> = emptyList(),
    val warnings: List<DiagnosticMessage> = emptyList(),
    val infos: List<DiagnosticMessage> = emptyList(),
) {
    public val hasErrors: Boolean get() = errors.isNotEmpty()
    public val isValid: Boolean get() = errors.isEmpty()
}
