package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class Settings(
    val defaultDistribution: String? = null,
    val strictVersionResolution: Boolean = false,
    val validation: ValidationSettings = ValidationSettings(),
    val toml: TomlSettings = TomlSettings(),
    val bom: BomSettings = BomSettings(),
    val bomCache: BomCacheSettings = BomCacheSettings(),
    val updateCheck: UpdateCheckSettings = UpdateCheckSettings(),
    val compatibilityAnalysis: CompatibilityAnalysisSettings = CompatibilityAnalysisSettings(),
    val securityCheck: SecurityCheckSettings = SecurityCheckSettings(),
    val licenseCheck: LicenseCheckSettings = LicenseCheckSettings(),
    val transitiveResolution: TransitiveResolutionSettings = TransitiveResolutionSettings(),
    val report: ReportSettings = ReportSettings(),
    val customSettings: Map<String, JsonElement> = emptyMap(),
)

@Serializable
public data class ValidationSettings(
    val onCompatibilityViolation: ValidationAction = ValidationAction.FAIL,
    val onDeprecatedLibrary: ValidationAction = ValidationAction.WARN,
    val onJdkMismatch: ValidationAction = ValidationAction.FAIL,
    val onUnresolvedVersion: ValidationAction = ValidationAction.WARN,
)

@Serializable
public data class TomlSettings(
    val filename: String = "libs.versions.toml",
    val includeComments: Boolean = true,
    val sortSections: Boolean = true,
    val useInlineVersions: Boolean = false,
)

@Serializable
public data class BomSettings(
    val groupId: String = "",
    val artifactId: String = "",
    val version: String = "",
    val includeOptionalDependencies: Boolean = false,
)

@Serializable
public data class BomCacheSettings(
    val enabled: Boolean = true,
    val directory: String = "",
    val ttlHours: Long = 24,
    val ttlSnapshotHours: Long = 1,
)

@Serializable
public data class UpdateCheckSettings(
    val enabled: Boolean = false,
    val excludePatterns: List<String> = emptyList(),
    val includePrerelease: Boolean = false,
    val repositories: List<String> = emptyList(),
)

@Serializable
public data class CompatibilityAnalysisSettings(
    val enabled: Boolean = false,
    val targetJdk: Int? = null,
    val failOnErrors: Boolean = true,
)

@Serializable
public data class SecurityCheckSettings(
    val enabled: Boolean = false,
    val failOnVulnerability: Severity = Severity.ERROR,
    val ignoreVulnerabilities: List<String> = emptyList(),
)

@Serializable
public data class LicenseCheckSettings(
    val enabled: Boolean = false,
    val allowedLicenses: List<String> = emptyList(),
    val deniedLicenses: List<String> = emptyList(),
    val failOnUnknown: Boolean = false,
)

@Serializable
public data class TransitiveResolutionSettings(
    val enabled: Boolean = false,
    val depth: Int? = null,
    val conflictResolution: ConflictResolutionStrategy = ConflictResolutionStrategy.HIGHEST,
    val includeOptional: Boolean = false,
)

@Serializable
public data class ReportSettings(
    val format: ReportFormat = ReportFormat.MARKDOWN,
    val outputDir: String = "build/reports/dependanger",
    val sections: List<ReportSection> = ReportSection.entries,
)
