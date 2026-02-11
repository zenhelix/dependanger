package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class DiagnosticMessage(
    val code: String,
    val message: String,
    val severity: Severity,
    val processorId: String?,
    val context: Map<String, String>,
)

@Serializable
public data class Diagnostics(
    val errors: List<DiagnosticMessage>,
    val warnings: List<DiagnosticMessage>,
    val infos: List<DiagnosticMessage>,
) {
    public val hasErrors: Boolean get() = errors.isNotEmpty()
    public val isValid: Boolean get() = errors.isEmpty()

    public operator fun plus(other: Diagnostics): Diagnostics = Diagnostics(
        errors = errors + other.errors,
        warnings = warnings + other.warnings,
        infos = infos + other.infos,
    )

    public companion object {
        public fun of(vararg messages: DiagnosticMessage): Diagnostics = Diagnostics(
            errors = messages.filter { it.severity == Severity.ERROR },
            warnings = messages.filter { it.severity == Severity.WARNING },
            infos = messages.filter { it.severity == Severity.INFO },
        )

        public fun error(code: String, message: String, processorId: String? = null, context: Map<String, String> = emptyMap()): Diagnostics =
            Diagnostics(
                errors = listOf(DiagnosticMessage(code, message, Severity.ERROR, processorId, context)),
                warnings = emptyList(),
                infos = emptyList(),
            )

        public fun warning(code: String, message: String, processorId: String? = null, context: Map<String, String> = emptyMap()): Diagnostics =
            Diagnostics(
                errors = emptyList(),
                warnings = listOf(DiagnosticMessage(code, message, Severity.WARNING, processorId, context)),
                infos = emptyList(),
            )

        public fun info(code: String, message: String, processorId: String? = null, context: Map<String, String> = emptyMap()): Diagnostics =
            Diagnostics(
                errors = emptyList(),
                warnings = emptyList(),
                infos = listOf(DiagnosticMessage(code, message, Severity.INFO, processorId, context)),
            )
    }
}
