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
) {
    public companion object {
        public val DEFAULT: Settings = Settings(
            defaultDistribution = null,
            strictVersionResolution = false,
            repositories = emptyList(),
            validation = ValidationSettings.DEFAULT,
            toml = TomlSettings.DEFAULT,
            bom = BomSettings.DEFAULT,
            bomCache = BomCacheSettings.DEFAULT,
            updateCheck = UpdateCheckSettings.DEFAULT,
            compatibilityAnalysis = CompatibilityAnalysisSettings.DEFAULT,
            securityCheck = SecurityCheckSettings.DEFAULT,
            licenseCheck = LicenseCheckSettings.DEFAULT,
            transitiveResolution = TransitiveResolutionSettings.DEFAULT,
            report = ReportSettings.DEFAULT,
            customSettings = emptyMap(),
        )
    }
}

@Serializable
public data class ValidationSettings(
    val onCompatibilityViolation: ValidationAction,
    val onDeprecatedLibrary: ValidationAction,
    val onJdkMismatch: ValidationAction,
    val onUnresolvedVersion: ValidationAction,
) {
    public companion object {
        public val DEFAULT: ValidationSettings = ValidationSettings(
            onCompatibilityViolation = ValidationAction.FAIL,
            onDeprecatedLibrary = ValidationAction.WARN,
            onJdkMismatch = ValidationAction.FAIL,
            onUnresolvedVersion = ValidationAction.WARN,
        )
    }
}

@Serializable
public data class TomlSettings(
    val filename: String,
    val includeComments: Boolean,
    val sortSections: Boolean,
    val useInlineVersions: Boolean,
) {
    public companion object {
        public val DEFAULT: TomlSettings = TomlSettings(
            filename = "libs.versions.toml",
            includeComments = true,
            sortSections = true,
            useInlineVersions = false,
        )
    }
}

@Serializable
public data class BomSettings(
    val groupId: String?,
    val artifactId: String?,
    val version: String?,
    val includeOptionalDependencies: Boolean,
) {
    public companion object {
        public val DEFAULT: BomSettings = BomSettings(
            groupId = null,
            artifactId = null,
            version = null,
            includeOptionalDependencies = false,
        )
    }
}

@Serializable
public data class BomCacheSettings(
    val enabled: Boolean,
    val directory: String?,
    val ttlHours: Long,
    val ttlSnapshotHours: Long,
) {
    public companion object {
        public val DEFAULT: BomCacheSettings = BomCacheSettings(
            enabled = true,
            directory = null,
            ttlHours = 24,
            ttlSnapshotHours = 1,
        )
    }
}

@Serializable
public data class UpdateCheckSettings(
    val enabled: Boolean,
    val excludePatterns: List<String>,
    val includePrerelease: Boolean,
    val repositories: List<Repository>,
    val timeout: Long,
    val parallelism: Int,
    val cacheDirectory: String?,
    val cacheTtlHours: Long,
) {
    public companion object {
        public val DEFAULT: UpdateCheckSettings = UpdateCheckSettings(
            enabled = false,
            excludePatterns = emptyList(),
            includePrerelease = false,
            repositories = emptyList(),
            timeout = 30_000,
            parallelism = 10,
            cacheDirectory = null,
            cacheTtlHours = 1,
        )
    }
}

@Serializable
public data class CompatibilityAnalysisSettings(
    val enabled: Boolean,
    val targetJdk: Int?,
    val failOnErrors: Boolean,
) {
    public companion object {
        public val DEFAULT: CompatibilityAnalysisSettings = CompatibilityAnalysisSettings(
            enabled = false,
            targetJdk = null,
            failOnErrors = true,
        )
    }
}

@Serializable
public data class SecurityCheckSettings(
    val enabled: Boolean,
    val failOnVulnerability: Severity,
    val ignoreVulnerabilities: List<String>,
) {
    public companion object {
        public val DEFAULT: SecurityCheckSettings = SecurityCheckSettings(
            enabled = false,
            failOnVulnerability = Severity.ERROR,
            ignoreVulnerabilities = emptyList(),
        )
    }
}

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
) {
    public companion object {
        public val DEFAULT: LicenseCheckSettings = LicenseCheckSettings(
            enabled = false,
            allowedLicenses = emptyList(),
            deniedLicenses = emptyList(),
            dualLicensePolicy = DualLicensePolicy.OR,
            failOnDenied = false,
            failOnUnknown = false,
            failOnCopyleft = false,
            warnOnCopyleft = true,
            warnOnUnknown = true,
            ignoreLibraries = emptyList(),
            includeTransitives = false,
        )
    }
}

@Serializable
public data class TransitiveResolutionSettings(
    val enabled: Boolean,
    val repositories: List<Repository>,
    val maxDepth: Int?,
    val maxTransitives: Int?,
    val conflictResolution: ConflictResolutionStrategy,
    val includeOptional: Boolean,
    val scopes: List<String>,
) {
    public companion object {
        public val DEFAULT: TransitiveResolutionSettings = TransitiveResolutionSettings(
            enabled = false,
            repositories = emptyList(),
            maxDepth = null,
            maxTransitives = null,
            conflictResolution = ConflictResolutionStrategy.HIGHEST,
            includeOptional = false,
            scopes = listOf("compile", "runtime"),
        )
    }
}

@Serializable
public data class ReportSettings(
    val format: ReportFormat,
    val outputDir: String,
    val sections: List<ReportSection>,
) {
    public companion object {
        public val DEFAULT: ReportSettings = ReportSettings(
            format = ReportFormat.MARKDOWN,
            outputDir = "build/reports/dependanger",
            sections = ReportSection.entries,
        )
    }
}
