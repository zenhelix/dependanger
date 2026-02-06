package io.github.zenhelix.dependanger.core.validation

import io.github.zenhelix.dependanger.core.model.Severity
import kotlinx.serialization.Serializable

@Serializable
public data class ValidationResult(
    val errors: List<ValidationMessage> = emptyList(),
    val warnings: List<ValidationMessage> = emptyList(),
    val infos: List<ValidationMessage> = emptyList(),
) {
    public val isValid: Boolean get() = errors.isEmpty()
}

@Serializable
public data class ValidationMessage(
    val code: String,
    val message: String,
    val severity: Severity,
    val path: String? = null,
)
