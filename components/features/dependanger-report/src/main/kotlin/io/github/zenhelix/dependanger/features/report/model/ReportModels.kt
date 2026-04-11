package io.github.zenhelix.dependanger.features.report.model

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import kotlinx.serialization.Serializable

@Serializable
public data class ReportLibrary(
    val alias: String,
    val coordinate: MavenCoordinate,
    val version: String?,
    val tags: Set<String>,
    val isDeprecated: Boolean,
    val isPlatform: Boolean,
    val constraints: List<ReportConstraint>? = null,
)

@Serializable
public data class ReportPlugin(
    val alias: String,
    val id: String,
    val version: String?,
)

@Serializable
public data class ReportBundle(
    val alias: String,
    val libraries: List<String>,
)

@Serializable
public data class ReportVersion(
    val alias: String,
    val value: String,
    val hasFallback: Boolean,
)

@Serializable
public data class ReportDeprecated(
    val alias: String,
    val reason: String?,
    val replacement: String?,
)

@Serializable
public data class ReportUpdate(
    val alias: String,
    val currentVersion: String,
    val availableVersion: String,
    val type: String,
)

@Serializable
public data class ReportCompatibilityIssue(
    val ruleId: String,
    val severity: String,
    val message: String,
    val suggestion: String?,
)

@Serializable
public data class ReportVulnerability(
    val id: String,
    val cveId: String?,
    val library: String,
    val severity: String,
    val cvssScore: Double?,
    val fixedVersion: String?,
    val summary: String,
)

@Serializable
public data class ReportLicense(
    val library: String,
    val licenseName: String,
    val status: String,
)

@Serializable
public data class ReportTransitives(
    val directCount: Int,
    val transitiveCount: Int,
    val conflictCount: Int,
    val conflicts: List<ReportConflict>,
)

@Serializable
public data class ReportConflict(
    val coordinate: String,
    val versions: List<String>,
)

@Serializable
public data class ReportConstraint(
    val type: String,
    val description: String,
)

@Serializable
public data class ReportValidation(
    val errors: List<ReportDiagnostic>,
    val warnings: List<ReportDiagnostic>,
)

@Serializable
public data class ReportDiagnostic(
    val code: String,
    val message: String,
)
