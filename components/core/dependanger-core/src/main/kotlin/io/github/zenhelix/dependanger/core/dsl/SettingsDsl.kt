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
import kotlinx.serialization.json.JsonElement

@DependangerDslMarker
public class SettingsDsl {
    private val _defaultDistribution = Trackable<String?>(null)
    public var defaultDistribution: String? by _defaultDistribution

    private val _strictVersionResolution = Trackable(false)
    public var strictVersionResolution: Boolean by _strictVersionResolution

    private val _repositories = Trackable<List<Repository>>(emptyList())
    public var repositories: List<Repository> by _repositories

    private val _validationSettings = Trackable(ValidationSettings.DEFAULT)
    public var validationSettings: ValidationSettings by _validationSettings

    private val _tomlSettings = Trackable(TomlSettings.DEFAULT)
    public var tomlSettings: TomlSettings by _tomlSettings

    private val _bomSettings = Trackable(BomSettings.DEFAULT)
    public var bomSettings: BomSettings by _bomSettings

    private val _bomCacheSettings = Trackable(BomCacheSettings.DEFAULT)
    public var bomCacheSettings: BomCacheSettings by _bomCacheSettings

    private val _updateCheckSettings = Trackable(UpdateCheckSettings.DEFAULT)
    public var updateCheckSettings: UpdateCheckSettings by _updateCheckSettings

    private val _compatibilityAnalysisSettings = Trackable(CompatibilityAnalysisSettings.DEFAULT)
    public var compatibilityAnalysisSettings: CompatibilityAnalysisSettings by _compatibilityAnalysisSettings

    private val _securityCheckSettings = Trackable(SecurityCheckSettings.DEFAULT)
    public var securityCheckSettings: SecurityCheckSettings by _securityCheckSettings

    private val _licenseCheckSettings = Trackable(LicenseCheckSettings.DEFAULT)
    public var licenseCheckSettings: LicenseCheckSettings by _licenseCheckSettings

    private val _transitiveResolutionSettings = Trackable(TransitiveResolutionSettings.DEFAULT)
    public var transitiveResolutionSettings: TransitiveResolutionSettings by _transitiveResolutionSettings

    private val _reportSettings = Trackable(ReportSettings.DEFAULT)
    public var reportSettings: ReportSettings by _reportSettings

    private val _customSettings = Trackable<Map<String, JsonElement>>(emptyMap())
    public var customSettings: Map<String, JsonElement> by _customSettings

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

    public fun applyTo(target: SettingsDsl) {
        if (_defaultDistribution.isSet) target.defaultDistribution = defaultDistribution
        if (_strictVersionResolution.isSet) target.strictVersionResolution = strictVersionResolution
        if (_repositories.isSet) target.repositories = repositories
        if (_validationSettings.isSet) target.validationSettings = validationSettings
        if (_tomlSettings.isSet) target.tomlSettings = tomlSettings
        if (_bomSettings.isSet) target.bomSettings = bomSettings
        if (_bomCacheSettings.isSet) target.bomCacheSettings = bomCacheSettings
        if (_updateCheckSettings.isSet) target.updateCheckSettings = updateCheckSettings
        if (_compatibilityAnalysisSettings.isSet) target.compatibilityAnalysisSettings = compatibilityAnalysisSettings
        if (_securityCheckSettings.isSet) target.securityCheckSettings = securityCheckSettings
        if (_licenseCheckSettings.isSet) target.licenseCheckSettings = licenseCheckSettings
        if (_transitiveResolutionSettings.isSet) target.transitiveResolutionSettings = transitiveResolutionSettings
        if (_reportSettings.isSet) target.reportSettings = reportSettings
        if (_customSettings.isSet) target.customSettings = customSettings
    }

    public fun mergeFrom(settings: Settings) {
        val defaults = Settings.DEFAULT
        if (settings.defaultDistribution != defaults.defaultDistribution) defaultDistribution = settings.defaultDistribution
        if (settings.strictVersionResolution != defaults.strictVersionResolution) strictVersionResolution = settings.strictVersionResolution
        if (settings.repositories != defaults.repositories) repositories = settings.repositories
        if (settings.validation != defaults.validation) validationSettings = settings.validation
        if (settings.toml != defaults.toml) tomlSettings = settings.toml
        if (settings.bom != defaults.bom) bomSettings = settings.bom
        if (settings.bomCache != defaults.bomCache) bomCacheSettings = settings.bomCache
        if (settings.updateCheck != defaults.updateCheck) updateCheckSettings = settings.updateCheck
        if (settings.compatibilityAnalysis != defaults.compatibilityAnalysis) compatibilityAnalysisSettings = settings.compatibilityAnalysis
        if (settings.securityCheck != defaults.securityCheck) securityCheckSettings = settings.securityCheck
        if (settings.licenseCheck != defaults.licenseCheck) licenseCheckSettings = settings.licenseCheck
        if (settings.transitiveResolution != defaults.transitiveResolution) transitiveResolutionSettings = settings.transitiveResolution
        if (settings.report != defaults.report) reportSettings = settings.report
        if (settings.customSettings != defaults.customSettings) customSettings = settings.customSettings
    }

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
        customSettings = customSettings,
    )
}

@DependangerDslMarker
public class ValidationSettingsDsl {
    public var onCompatibilityViolation: ValidationAction = ValidationSettings.DEFAULT.onCompatibilityViolation
    public var onDeprecatedLibrary: ValidationAction = ValidationSettings.DEFAULT.onDeprecatedLibrary
    public var onJdkMismatch: ValidationAction = ValidationSettings.DEFAULT.onJdkMismatch
    public var onUnresolvedVersion: ValidationAction = ValidationSettings.DEFAULT.onUnresolvedVersion

    public fun toSettings(): ValidationSettings = ValidationSettings(
        onCompatibilityViolation = onCompatibilityViolation,
        onDeprecatedLibrary = onDeprecatedLibrary,
        onJdkMismatch = onJdkMismatch,
        onUnresolvedVersion = onUnresolvedVersion,
    )
}

@DependangerDslMarker
public class TomlSettingsDsl {
    public var filename: String = TomlSettings.DEFAULT.filename
    public var includeComments: Boolean = TomlSettings.DEFAULT.includeComments
    public var sortSections: Boolean = TomlSettings.DEFAULT.sortSections
    public var useInlineVersions: Boolean = TomlSettings.DEFAULT.useInlineVersions
    public var includeDeprecationComments: Boolean = TomlSettings.DEFAULT.includeDeprecationComments

    public fun toSettings(): TomlSettings = TomlSettings(
        filename = filename,
        includeComments = includeComments,
        sortSections = sortSections,
        useInlineVersions = useInlineVersions,
        includeDeprecationComments = includeDeprecationComments,
    )
}

@DependangerDslMarker
public class BomSettingsDsl {
    public var groupId: String? = BomSettings.DEFAULT.groupId
    public var artifactId: String? = BomSettings.DEFAULT.artifactId
    public var version: String? = BomSettings.DEFAULT.version
    public var name: String? = BomSettings.DEFAULT.name
    public var description: String? = BomSettings.DEFAULT.description
    public var includeOptionalDependencies: Boolean = BomSettings.DEFAULT.includeOptionalDependencies
    public var prettyPrint: Boolean = BomSettings.DEFAULT.prettyPrint
    public var includeDeprecationComments: Boolean = BomSettings.DEFAULT.includeDeprecationComments

    public fun toSettings(): BomSettings = BomSettings(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        name = name,
        description = description,
        includeOptionalDependencies = includeOptionalDependencies,
        prettyPrint = prettyPrint,
        includeDeprecationComments = includeDeprecationComments,
    )
}

@DependangerDslMarker
public class BomCacheSettingsDsl {
    public var enabled: Boolean = BomCacheSettings.DEFAULT.enabled
    public var directory: String? = BomCacheSettings.DEFAULT.directory
    public var ttlHours: Long = BomCacheSettings.DEFAULT.ttlHours
    public var ttlSnapshotHours: Long = BomCacheSettings.DEFAULT.ttlSnapshotHours

    public fun toSettings(): BomCacheSettings = BomCacheSettings(
        enabled = enabled,
        directory = directory,
        ttlHours = ttlHours,
        ttlSnapshotHours = ttlSnapshotHours,
    )
}

@DependangerDslMarker
public class UpdateCheckSettingsDsl {
    public var enabled: Boolean = UpdateCheckSettings.DEFAULT.enabled
    public var excludePatterns: List<String> = UpdateCheckSettings.DEFAULT.excludePatterns
    public var includePrerelease: Boolean = UpdateCheckSettings.DEFAULT.includePrerelease
    public var repositories: List<Repository> = UpdateCheckSettings.DEFAULT.repositories
    public var timeout: Long = UpdateCheckSettings.DEFAULT.timeout
    public var parallelism: Int = UpdateCheckSettings.DEFAULT.parallelism
    public var cacheDirectory: String? = UpdateCheckSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = UpdateCheckSettings.DEFAULT.cacheTtlHours

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
    public var enabled: Boolean = CompatibilityAnalysisSettings.DEFAULT.enabled
    public var targetJdk: Int? = CompatibilityAnalysisSettings.DEFAULT.targetJdk
    public var targetKotlin: String? = CompatibilityAnalysisSettings.DEFAULT.targetKotlin
    public var minSeverity: Severity = CompatibilityAnalysisSettings.DEFAULT.minSeverity
    public var failOnErrors: Boolean = CompatibilityAnalysisSettings.DEFAULT.failOnErrors

    public fun toSettings(): CompatibilityAnalysisSettings = CompatibilityAnalysisSettings(
        enabled = enabled,
        targetJdk = targetJdk,
        targetKotlin = targetKotlin,
        minSeverity = minSeverity,
        failOnErrors = failOnErrors,
    )
}

@DependangerDslMarker
public class SecurityCheckSettingsDsl {
    public var enabled: Boolean = SecurityCheckSettings.DEFAULT.enabled
    public var failOnVulnerability: Severity = SecurityCheckSettings.DEFAULT.failOnVulnerability
    public var minSeverity: String = SecurityCheckSettings.DEFAULT.minSeverity
    public var ignoreVulnerabilities: List<String> = SecurityCheckSettings.DEFAULT.ignoreVulnerabilities
    public var timeout: Long = SecurityCheckSettings.DEFAULT.timeout
    public var parallelism: Int = SecurityCheckSettings.DEFAULT.parallelism
    public var cacheDirectory: String? = SecurityCheckSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = SecurityCheckSettings.DEFAULT.cacheTtlHours

    public fun toSettings(): SecurityCheckSettings = SecurityCheckSettings(
        enabled = enabled,
        failOnVulnerability = failOnVulnerability,
        minSeverity = minSeverity,
        ignoreVulnerabilities = ignoreVulnerabilities,
        timeout = timeout,
        parallelism = parallelism,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

@DependangerDslMarker
public class LicenseCheckSettingsDsl {
    public var enabled: Boolean = LicenseCheckSettings.DEFAULT.enabled
    public var allowedLicenses: List<String> = LicenseCheckSettings.DEFAULT.allowedLicenses
    public var deniedLicenses: List<String> = LicenseCheckSettings.DEFAULT.deniedLicenses
    public var dualLicensePolicy: DualLicensePolicy = LicenseCheckSettings.DEFAULT.dualLicensePolicy
    public var failOnDenied: Boolean = LicenseCheckSettings.DEFAULT.failOnDenied
    public var failOnUnknown: Boolean = LicenseCheckSettings.DEFAULT.failOnUnknown
    public var failOnCopyleft: Boolean = LicenseCheckSettings.DEFAULT.failOnCopyleft
    public var warnOnCopyleft: Boolean = LicenseCheckSettings.DEFAULT.warnOnCopyleft
    public var warnOnUnknown: Boolean = LicenseCheckSettings.DEFAULT.warnOnUnknown
    public var ignoreLibraries: List<String> = LicenseCheckSettings.DEFAULT.ignoreLibraries
    public var includeTransitives: Boolean = LicenseCheckSettings.DEFAULT.includeTransitives
    public var timeout: Long = LicenseCheckSettings.DEFAULT.timeout
    public var parallelism: Int = LicenseCheckSettings.DEFAULT.parallelism
    public var cacheDirectory: String? = LicenseCheckSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = LicenseCheckSettings.DEFAULT.cacheTtlHours

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
        timeout = timeout,
        parallelism = parallelism,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

@DependangerDslMarker
public class TransitiveResolutionSettingsDsl {
    public var enabled: Boolean = TransitiveResolutionSettings.DEFAULT.enabled
    public var repositories: List<Repository> = TransitiveResolutionSettings.DEFAULT.repositories
    public var maxDepth: Int? = TransitiveResolutionSettings.DEFAULT.maxDepth
    public var maxTransitives: Int? = TransitiveResolutionSettings.DEFAULT.maxTransitives
    public var conflictResolution: ConflictResolutionStrategy = TransitiveResolutionSettings.DEFAULT.conflictResolution
    public var includeOptional: Boolean = TransitiveResolutionSettings.DEFAULT.includeOptional
    public var scopes: List<String> = TransitiveResolutionSettings.DEFAULT.scopes
    public var cacheDirectory: String? = TransitiveResolutionSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = TransitiveResolutionSettings.DEFAULT.cacheTtlHours

    public fun toSettings(): TransitiveResolutionSettings = TransitiveResolutionSettings(
        enabled = enabled,
        repositories = repositories,
        maxDepth = maxDepth,
        maxTransitives = maxTransitives,
        conflictResolution = conflictResolution,
        includeOptional = includeOptional,
        scopes = scopes,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

@DependangerDslMarker
public class ReportSettingsDsl {
    public var format: ReportFormat = ReportSettings.DEFAULT.format
    public var outputDir: String = ReportSettings.DEFAULT.outputDir
    public var sections: List<ReportSection> = ReportSettings.DEFAULT.sections

    public fun toSettings(): ReportSettings = ReportSettings(
        format = format,
        outputDir = outputDir,
        sections = sections,
    )
}
