package io.github.zenhelix.dependanger.features.analysis

import io.github.zenhelix.dependanger.core.model.Severity
import kotlinx.serialization.Serializable

@Serializable
public data class CompatibilityIssue(
    val ruleId: String,
    val message: String,
    val severity: Severity,
    val affectedLibraries: List<String> = emptyList(),
)
