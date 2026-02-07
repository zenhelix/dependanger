package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class DiagnosticMessage(
    val code: String,
    val message: String,
    val severity: Severity,
    val processorId: String? = null,
    val context: Map<String, String> = emptyMap(),
)

@Serializable
public data class Diagnostics(
    val errors: List<DiagnosticMessage> = emptyList(),
    val warnings: List<DiagnosticMessage> = emptyList(),
    val infos: List<DiagnosticMessage> = emptyList(),
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
            Diagnostics(errors = listOf(DiagnosticMessage(code, message, Severity.ERROR, processorId, context)))

        public fun warning(code: String, message: String, processorId: String? = null, context: Map<String, String> = emptyMap()): Diagnostics =
            Diagnostics(warnings = listOf(DiagnosticMessage(code, message, Severity.WARNING, processorId, context)))

        public fun info(code: String, message: String, processorId: String? = null, context: Map<String, String> = emptyMap()): Diagnostics =
            Diagnostics(infos = listOf(DiagnosticMessage(code, message, Severity.INFO, processorId, context)))
    }
}
