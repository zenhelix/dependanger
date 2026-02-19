package io.github.zenhelix.dependanger.features.report.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ReportData(
    val schemaVersion: String,
    val generatedAt: String,
    val distribution: String?,
    val summary: ReportSummary,
    val libraries: List<ReportLibrary>?,
    val plugins: List<ReportPlugin>?,
    val bundles: List<ReportBundle>?,
    val versions: List<ReportVersion>?,
    val updates: List<ReportUpdate>?,
    val compatibility: List<ReportCompatibilityIssue>?,
    val vulnerabilities: List<ReportVulnerability>?,
    val deprecated: List<ReportDeprecated>?,
    val licenses: List<ReportLicense>?,
    val transitives: ReportTransitives?,
    val constraints: List<ReportConstraint>?,
    val validation: ReportValidation?,
)

@Serializable
internal data class ReportSummary(
    val libraryCount: Int,
    val pluginCount: Int,
    val bundleCount: Int,
    val deprecatedCount: Int,
    val updatesAvailable: Int,
    val vulnerabilitiesCount: Int,
    val criticalVulnerabilities: Int,
    val highVulnerabilities: Int,
    val deniedLicenses: Int,
    val unknownLicenses: Int,
    val compatibilityIssues: Int,
    val validationErrors: Int,
    val validationWarnings: Int,
)
