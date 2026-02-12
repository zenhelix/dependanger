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
        public val EMPTY: Diagnostics = Diagnostics(
            errors = emptyList(),
            warnings = emptyList(),
            infos = emptyList(),
        )

        public fun of(vararg messages: DiagnosticMessage): Diagnostics {
            val grouped = messages.groupBy { it.severity }
            return Diagnostics(
                errors = grouped[Severity.ERROR].orEmpty(),
                warnings = grouped[Severity.WARNING].orEmpty(),
                infos = grouped[Severity.INFO].orEmpty(),
            )
        }

        public fun error(code: String, message: String, processorId: String?, context: Map<String, String>): Diagnostics =
            Diagnostics(
                errors = listOf(DiagnosticMessage(code, message, Severity.ERROR, processorId, context)),
                warnings = emptyList(),
                infos = emptyList(),
            )

        public fun warning(code: String, message: String, processorId: String?, context: Map<String, String>): Diagnostics =
            Diagnostics(
                errors = emptyList(),
                warnings = listOf(DiagnosticMessage(code, message, Severity.WARNING, processorId, context)),
                infos = emptyList(),
            )

        public fun info(code: String, message: String, processorId: String?, context: Map<String, String>): Diagnostics =
            Diagnostics(
                errors = emptyList(),
                warnings = emptyList(),
                infos = listOf(DiagnosticMessage(code, message, Severity.INFO, processorId, context)),
            )
    }
}
