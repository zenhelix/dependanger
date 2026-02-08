package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.BomCacheSettings
import io.github.zenhelix.dependanger.core.model.BomSettings
import io.github.zenhelix.dependanger.core.model.CompatibilityAnalysisSettings
import io.github.zenhelix.dependanger.core.model.ConflictResolutionStrategy
import io.github.zenhelix.dependanger.core.model.DualLicensePolicy
import io.github.zenhelix.dependanger.core.model.LicenseCheckSettings
import io.github.zenhelix.dependanger.core.model.ReportFormat
import io.github.zenhelix.dependanger.core.model.ReportSection
import io.github.zenhelix.dependanger.core.model.ReportSettings
import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.core.model.SecurityCheckSettings
import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.TomlSettings
import io.github.zenhelix.dependanger.core.model.TransitiveResolutionSettings
import io.github.zenhelix.dependanger.core.model.UpdateCheckSettings
import io.github.zenhelix.dependanger.core.model.ValidationAction
import io.github.zenhelix.dependanger.core.model.ValidationSettings

@DependangerDslMarker
public class SettingsDsl {
    public var defaultDistribution: String? = null
    public var strictVersionResolution: Boolean = false
    public var repositories: List<Repository> = emptyList()

    public var validationSettings: ValidationSettings = ValidationSettings()
    public var tomlSettings: TomlSettings = TomlSettings()
    public var bomSettings: BomSettings = BomSettings()
    public var bomCacheSettings: BomCacheSettings = BomCacheSettings()
    public var updateCheckSettings: UpdateCheckSettings = UpdateCheckSettings()
    public var compatibilityAnalysisSettings: CompatibilityAnalysisSettings = CompatibilityAnalysisSettings()
    public var securityCheckSettings: SecurityCheckSettings = SecurityCheckSettings()
    public var licenseCheckSettings: LicenseCheckSettings = LicenseCheckSettings()
    public var transitiveResolutionSettings: TransitiveResolutionSettings = TransitiveResolutionSettings()
    public var reportSettings: ReportSettings = ReportSettings()

    public fun validation(block: ValidationSettingsDsl.() -> Unit) {
        val dsl = ValidationSettingsDsl().apply(block)
        validationSettings = dsl.toSettings()
    }

    public fun toml(block: TomlSettingsDsl.() -> Unit) {
        val dsl = TomlSettingsDsl().apply(block)
        tomlSettings = dsl.toSettings()
    }

    public fun bom(block: BomSettingsDsl.() -> Unit) {
        val dsl = BomSettingsDsl().apply(block)
        bomSettings = dsl.toSettings()
    }

    public fun bomCache(block: BomCacheSettingsDsl.() -> Unit) {
        val dsl = BomCacheSettingsDsl().apply(block)
        bomCacheSettings = dsl.toSettings()
    }

    public fun updateCheck(block: UpdateCheckSettingsDsl.() -> Unit) {
        val dsl = UpdateCheckSettingsDsl().apply(block)
        updateCheckSettings = dsl.toSettings()
    }

    public fun compatibilityAnalysis(block: CompatibilityAnalysisSettingsDsl.() -> Unit) {
        val dsl = CompatibilityAnalysisSettingsDsl().apply(block)
        compatibilityAnalysisSettings = dsl.toSettings()
    }

    public fun securityCheck(block: SecurityCheckSettingsDsl.() -> Unit) {
        val dsl = SecurityCheckSettingsDsl().apply(block)
        securityCheckSettings = dsl.toSettings()
    }

    public fun licenseCheck(block: LicenseCheckSettingsDsl.() -> Unit) {
        val dsl = LicenseCheckSettingsDsl().apply(block)
        licenseCheckSettings = dsl.toSettings()
    }

    public fun transitiveResolution(block: TransitiveResolutionSettingsDsl.() -> Unit) {
        val dsl = TransitiveResolutionSettingsDsl().apply(block)
        transitiveResolutionSettings = dsl.toSettings()
    }

    public fun report(block: ReportSettingsDsl.() -> Unit) {
        val dsl = ReportSettingsDsl().apply(block)
        reportSettings = dsl.toSettings()
    }

    public fun mergeFrom(settings: Settings): Unit = TODO()

    public fun toSettings(): Settings = Settings(
        defaultDistribution = defaultDistribution,
        strictVersionResolution = strictVersionResolution,
        repositories = repositories,
        validation = validationSettings,
        toml = tomlSettings,
        bom = bomSettings,
        bomCache = bomCacheSettings,
        updateCheck = updateCheckSettings,
        compatibilityAnalysis = compatibilityAnalysisSettings,
        securityCheck = securityCheckSettings,
        licenseCheck = licenseCheckSettings,
        transitiveResolution = transitiveResolutionSettings,
        report = reportSettings,
    )
}

@DependangerDslMarker
public class ValidationSettingsDsl {
    public var onCompatibilityViolation: ValidationAction = ValidationAction.FAIL
    public var onDeprecatedLibrary: ValidationAction = ValidationAction.WARN
    public var onJdkMismatch: ValidationAction = ValidationAction.FAIL
    public var onUnresolvedVersion: ValidationAction = ValidationAction.WARN

    public fun toSettings(): ValidationSettings = ValidationSettings(
        onCompatibilityViolation = onCompatibilityViolation,
        onDeprecatedLibrary = onDeprecatedLibrary,
        onJdkMismatch = onJdkMismatch,
        onUnresolvedVersion = onUnresolvedVersion,
    )
}

@DependangerDslMarker
public class TomlSettingsDsl {
    public var filename: String = "libs.versions.toml"
    public var includeComments: Boolean = true
    public var sortSections: Boolean = true
    public var useInlineVersions: Boolean = false

    public fun toSettings(): TomlSettings = TomlSettings(
        filename = filename,
        includeComments = includeComments,
        sortSections = sortSections,
        useInlineVersions = useInlineVersions,
    )
}

@DependangerDslMarker
public class BomSettingsDsl {
    public var groupId: String = ""
    public var artifactId: String = ""
    public var version: String = ""
    public var includeOptionalDependencies: Boolean = false

    public fun toSettings(): BomSettings = BomSettings(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        includeOptionalDependencies = includeOptionalDependencies,
    )
}

@DependangerDslMarker
public class BomCacheSettingsDsl {
    public var enabled: Boolean = true
    public var directory: String = ""
    public var ttlHours: Long = 24
    public var ttlSnapshotHours: Long = 1

    public fun toSettings(): BomCacheSettings = BomCacheSettings(
        enabled = enabled,
        directory = directory,
        ttlHours = ttlHours,
        ttlSnapshotHours = ttlSnapshotHours,
    )
}

@DependangerDslMarker
public class UpdateCheckSettingsDsl {
    public var enabled: Boolean = false
    public var excludePatterns: List<String> = emptyList()
    public var includePrerelease: Boolean = false
    public var repositories: List<Repository> = emptyList()
    public var timeout: Long = 30_000
    public var parallelism: Int = 10
    public var cacheDirectory: String = ""
    public var cacheTtlHours: Long = 1

    public fun toSettings(): UpdateCheckSettings = UpdateCheckSettings(
        enabled = enabled,
        excludePatterns = excludePatterns,
        includePrerelease = includePrerelease,
        repositories = repositories,
        timeout = timeout,
        parallelism = parallelism,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

@DependangerDslMarker
public class CompatibilityAnalysisSettingsDsl {
    public var enabled: Boolean = false
    public var targetJdk: Int? = null
    public var failOnErrors: Boolean = true

    public fun toSettings(): CompatibilityAnalysisSettings = CompatibilityAnalysisSettings(
        enabled = enabled,
        targetJdk = targetJdk,
        failOnErrors = failOnErrors,
    )
}

@DependangerDslMarker
public class SecurityCheckSettingsDsl {
    public var enabled: Boolean = false
    public var failOnVulnerability: Severity = Severity.ERROR
    public var ignoreVulnerabilities: List<String> = emptyList()

    public fun toSettings(): SecurityCheckSettings = SecurityCheckSettings(
        enabled = enabled,
        failOnVulnerability = failOnVulnerability,
        ignoreVulnerabilities = ignoreVulnerabilities,
    )
}

@DependangerDslMarker
public class LicenseCheckSettingsDsl {
    public var enabled: Boolean = false
    public var allowedLicenses: List<String> = emptyList()
    public var deniedLicenses: List<String> = emptyList()
    public var dualLicensePolicy: DualLicensePolicy = DualLicensePolicy.OR
    public var failOnDenied: Boolean = false
    public var failOnUnknown: Boolean = false
    public var failOnCopyleft: Boolean = false
    public var warnOnCopyleft: Boolean = true
    public var warnOnUnknown: Boolean = true
    public var ignoreLibraries: List<String> = emptyList()
    public var includeTransitives: Boolean = false

    public fun toSettings(): LicenseCheckSettings = LicenseCheckSettings(
        enabled = enabled,
        allowedLicenses = allowedLicenses,
        deniedLicenses = deniedLicenses,
        dualLicensePolicy = dualLicensePolicy,
        failOnDenied = failOnDenied,
        failOnUnknown = failOnUnknown,
        failOnCopyleft = failOnCopyleft,
        warnOnCopyleft = warnOnCopyleft,
        warnOnUnknown = warnOnUnknown,
        ignoreLibraries = ignoreLibraries,
        includeTransitives = includeTransitives,
    )
}

@DependangerDslMarker
public class TransitiveResolutionSettingsDsl {
    public var enabled: Boolean = false
    public var repositories: List<Repository> = emptyList()
    public var maxDepth: Int? = null
    public var maxTransitives: Int? = null
    public var conflictResolution: ConflictResolutionStrategy = ConflictResolutionStrategy.HIGHEST
    public var includeOptional: Boolean = false
    public var scopes: List<String> = listOf("compile", "runtime")

    public fun toSettings(): TransitiveResolutionSettings = TransitiveResolutionSettings(
        enabled = enabled,
        repositories = repositories,
        maxDepth = maxDepth,
        maxTransitives = maxTransitives,
        conflictResolution = conflictResolution,
        includeOptional = includeOptional,
        scopes = scopes,
    )
}

@DependangerDslMarker
public class ReportSettingsDsl {
    public var format: ReportFormat = ReportFormat.MARKDOWN
    public var outputDir: String = "build/reports/dependanger"
    public var sections: List<ReportSection> = ReportSection.entries

    public fun toSettings(): ReportSettings = ReportSettings(
        format = format,
        outputDir = outputDir,
        sections = sections,
    )
}
