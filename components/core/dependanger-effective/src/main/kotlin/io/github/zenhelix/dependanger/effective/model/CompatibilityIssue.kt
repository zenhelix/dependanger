package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import kotlinx.serialization.Serializable

@Serializable
public data class CompatibilityIssue(
    val ruleId: String,
    val message: String,
    val severity: Severity,
    val affectedLibraries: List<String>,
    val suggestion: String?,
)

public fun CompatibilityIssue.toDiagnostics(
    code: String,
    processorId: String,
    message: String,
    context: Map<String, String>,
): Diagnostics = when (severity) {
    Severity.ERROR   -> Diagnostics.error(code = code, message = message, processorId = processorId, context = context)
    Severity.WARNING -> Diagnostics.warning(code = code, message = message, processorId = processorId, context = context)
    Severity.INFO    -> Diagnostics.info(code = code, message = message, processorId = processorId, context = context)
}
