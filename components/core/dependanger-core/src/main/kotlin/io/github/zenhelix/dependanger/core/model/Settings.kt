package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class Settings(
    val defaultDistribution: String?,
    val strictVersionResolution: Boolean,
    val repositories: List<Repository>,
    val validation: ValidationSettings,
    val toml: TomlSettings,
    val bom: BomSettings,
    val bomCache: BomCacheSettings,
    val updateCheck: UpdateCheckSettings,
    val compatibilityAnalysis: CompatibilityAnalysisSettings,
    val securityCheck: SecurityCheckSettings,
    val licenseCheck: LicenseCheckSettings,
    val transitiveResolution: TransitiveResolutionSettings,
    val report: ReportSettings,
    val customSettings: Map<String, JsonElement>,
)

@Serializable
public data class ValidationSettings(
    val onCompatibilityViolation: ValidationAction,
    val onDeprecatedLibrary: ValidationAction,
    val onJdkMismatch: ValidationAction,
    val onUnresolvedVersion: ValidationAction,
)

@Serializable
public data class TomlSettings(
    val filename: String,
    val includeComments: Boolean,
    val sortSections: Boolean,
    val useInlineVersions: Boolean,
)

@Serializable
public data class BomSettings(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val includeOptionalDependencies: Boolean,
)

@Serializable
public data class BomCacheSettings(
    val enabled: Boolean,
    val directory: String,
    val ttlHours: Long,
    val ttlSnapshotHours: Long,
)

@Serializable
public data class UpdateCheckSettings(
    val enabled: Boolean,
    val excludePatterns: List<String>,
    val includePrerelease: Boolean,
    val repositories: List<Repository>,
    val timeout: Long,
    val parallelism: Int,
    val cacheDirectory: String,
    val cacheTtlHours: Long,
)

@Serializable
public data class CompatibilityAnalysisSettings(
    val enabled: Boolean,
    val targetJdk: Int?,
    val failOnErrors: Boolean,
)

@Serializable
public data class SecurityCheckSettings(
    val enabled: Boolean,
    val failOnVulnerability: Severity,
    val ignoreVulnerabilities: List<String>,
)

@Serializable
public data class LicenseCheckSettings(
    val enabled: Boolean,
    val allowedLicenses: List<String>,
    val deniedLicenses: List<String>,
    val dualLicensePolicy: DualLicensePolicy,
    val failOnDenied: Boolean,
    val failOnUnknown: Boolean,
    val failOnCopyleft: Boolean,
    val warnOnCopyleft: Boolean,
    val warnOnUnknown: Boolean,
    val ignoreLibraries: List<String>,
    val includeTransitives: Boolean,
)

@Serializable
public data class TransitiveResolutionSettings(
    val enabled: Boolean,
    val repositories: List<Repository>,
    val maxDepth: Int?,
    val maxTransitives: Int?,
    val conflictResolution: ConflictResolutionStrategy,
    val includeOptional: Boolean,
    val scopes: List<String>,
)

@Serializable
public data class ReportSettings(
    val format: ReportFormat,
    val outputDir: String,
    val sections: List<ReportSection>,
)
