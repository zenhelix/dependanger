package io.github.zenhelix.dependanger.features.report.mapper

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectivePlugin
import io.github.zenhelix.dependanger.effective.model.compatibilityIssues
import io.github.zenhelix.dependanger.effective.spi.ReportSection
import io.github.zenhelix.dependanger.feature.model.license.LicenseCategory
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolation
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationType
import io.github.zenhelix.dependanger.feature.model.license.licenseViolations
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import io.github.zenhelix.dependanger.feature.model.security.vulnerabilities
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflict
import io.github.zenhelix.dependanger.feature.model.transitive.flatDependencies
import io.github.zenhelix.dependanger.feature.model.transitive.versionConflicts
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import io.github.zenhelix.dependanger.feature.model.updates.updates
import io.github.zenhelix.dependanger.features.report.model.ReportBundle
import io.github.zenhelix.dependanger.features.report.model.ReportCompatibilityIssue
import io.github.zenhelix.dependanger.features.report.model.ReportConflict
import io.github.zenhelix.dependanger.features.report.model.ReportConstraint
import io.github.zenhelix.dependanger.features.report.model.ReportData
import io.github.zenhelix.dependanger.features.report.model.ReportDeprecated
import io.github.zenhelix.dependanger.features.report.model.ReportDiagnostic
import io.github.zenhelix.dependanger.features.report.model.ReportLibrary
import io.github.zenhelix.dependanger.features.report.model.ReportLicense
import io.github.zenhelix.dependanger.features.report.model.ReportPlugin
import io.github.zenhelix.dependanger.features.report.model.ReportSummary
import io.github.zenhelix.dependanger.features.report.model.ReportTransitives
import io.github.zenhelix.dependanger.features.report.model.ReportUpdate
import io.github.zenhelix.dependanger.features.report.model.ReportValidation
import io.github.zenhelix.dependanger.features.report.model.ReportVersion
import io.github.zenhelix.dependanger.features.report.model.ReportVulnerability
import java.time.Instant
import java.time.format.DateTimeFormatter

internal object ReportDataMapper {

    private const val REPORT_SCHEMA_VERSION: String = "1.0"
    private val utcFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    fun buildReportData(
        effective: EffectiveMetadata,
        sections: List<ReportSection>,
        originalMetadata: DependangerMetadata?,
        summaryOnly: Boolean,
    ): ReportData {
        fun <T> section(section: ReportSection, block: () -> T?): T? =
            if (!summaryOnly && section in sections) block() else null

        val allUpdates = effective.updates
        val allVulnerabilities = effective.vulnerabilities
        val allCompatIssues = effective.compatibilityIssues
        val allLicenseViolations = effective.licenseViolations

        val summary = buildSummary(
            effective = effective,
            allUpdates = allUpdates,
            allVulnerabilities = allVulnerabilities,
            allCompatIssues = allCompatIssues,
            allLicenseViolations = allLicenseViolations,
        )

        return ReportData(
            schemaVersion = REPORT_SCHEMA_VERSION,
            generatedAt = utcFormatter.format(Instant.now()),
            distribution = effective.distribution,
            summary = summary,
            libraries = section(ReportSection.LIBRARIES) {
                effective.libraries.values.map { it.toReportLibrary() }
            },
            plugins = section(ReportSection.PLUGINS) {
                effective.plugins.values.map { it.toReportPlugin() }
            },
            bundles = section(ReportSection.BUNDLES) {
                effective.bundles.values.map { ReportBundle(alias = it.alias, libraries = it.libraries) }
            },
            versions = section(ReportSection.VERSIONS) {
                buildVersions(effective, originalMetadata)
            },
            updates = section(ReportSection.UPDATES) {
                allUpdates.ifEmpty { null }?.map { it.toReportUpdate() }
            },
            compatibility = section(ReportSection.COMPATIBILITY) {
                allCompatIssues.ifEmpty { null }?.map { it.toReportCompatibilityIssue() }
            },
            vulnerabilities = section(ReportSection.VULNERABILITIES) {
                allVulnerabilities.ifEmpty { null }?.map { it.toReportVulnerability() }
            },
            deprecated = section(ReportSection.DEPRECATED) {
                effective.libraries.values
                    .filter { it.isDeprecated }
                    .map { it.toReportDeprecated() }
                    .ifEmpty { null }
            },
            licenses = section(ReportSection.LICENSES) {
                allLicenseViolations.ifEmpty { null }?.map { it.toReportLicense() }
            },
            transitives = section(ReportSection.TRANSITIVES) {
                buildTransitives(effective)
            },
            constraints = section(ReportSection.CONSTRAINTS) {
                originalMetadata?.constraints
                    ?.map { it.toReportConstraint() }
                    ?.ifEmpty { null }
            },
            validation = section(ReportSection.VALIDATION) {
                buildValidation(effective)
            },
        )
    }

    private fun buildSummary(
        effective: EffectiveMetadata,
        allUpdates: List<UpdateAvailableInfo>,
        allVulnerabilities: List<VulnerabilityInfo>,
        allCompatIssues: List<CompatibilityIssue>,
        allLicenseViolations: List<LicenseViolation>,
    ): ReportSummary = ReportSummary(
        libraryCount = effective.libraries.size,
        pluginCount = effective.plugins.size,
        bundleCount = effective.bundles.size,
        deprecatedCount = effective.libraries.values.count { it.isDeprecated },
        updatesAvailable = allUpdates.size,
        vulnerabilitiesCount = allVulnerabilities.size,
        criticalVulnerabilities = allVulnerabilities.count { it.severity == VulnerabilitySeverity.CRITICAL },
        highVulnerabilities = allVulnerabilities.count { it.severity == VulnerabilitySeverity.HIGH },
        deniedLicenses = allLicenseViolations.count { it.violationType == LicenseViolationType.DENIED },
        unknownLicenses = allLicenseViolations.count { it.category == LicenseCategory.UNKNOWN },
        compatibilityIssues = allCompatIssues.size,
        validationErrors = effective.diagnostics.errors.size,
        validationWarnings = effective.diagnostics.warnings.size,
    )

    private fun buildVersions(
        effective: EffectiveMetadata,
        originalMetadata: DependangerMetadata?,
    ): List<ReportVersion>? {
        val versions = effective.versions.values.map { resolved ->
            val hasFallback = originalMetadata?.versions
                ?.find { it.name == resolved.alias }
                ?.fallbacks
                ?.isNotEmpty() ?: false
            ReportVersion(
                alias = resolved.alias,
                value = resolved.value,
                hasFallback = hasFallback,
            )
        }
        return versions.ifEmpty { null }
    }

    private fun buildTransitives(effective: EffectiveMetadata): ReportTransitives? {
        val flatDeps = effective.flatDependencies
        val conflicts = effective.versionConflicts
        if (flatDeps.isEmpty() && conflicts.isEmpty()) return null

        val directCount = flatDeps.count { it.isDirectDependency }
        val transitiveCount = flatDeps.size - directCount

        return ReportTransitives(
            directCount = directCount,
            transitiveCount = transitiveCount,
            conflictCount = conflicts.size,
            conflicts = conflicts.map { it.toReportConflict() },
        )
    }

    private fun buildValidation(effective: EffectiveMetadata): ReportValidation =
        ReportValidation(
            errors = effective.diagnostics.errors.map { ReportDiagnostic(code = it.code, message = it.message) },
            warnings = effective.diagnostics.warnings.map { ReportDiagnostic(code = it.code, message = it.message) },
        )

    private fun EffectiveLibrary.toReportLibrary(): ReportLibrary = ReportLibrary(
        alias = alias,
        coordinate = coordinate,
        version = version.valueOrNull,
        tags = tags,
        isDeprecated = isDeprecated,
        isPlatform = isPlatform,
        constraints = constraints
            .ifEmpty { null }
            ?.map { it.toReportConstraint() },
    )

    private fun EffectivePlugin.toReportPlugin(): ReportPlugin = ReportPlugin(
        alias = alias,
        id = id,
        version = version.valueOrNull,
    )

    private fun EffectiveLibrary.toReportDeprecated(): ReportDeprecated = ReportDeprecated(
        alias = alias,
        reason = deprecation?.message,
        replacement = deprecation?.replacedBy,
    )

    private fun UpdateAvailableInfo.toReportUpdate(): ReportUpdate = ReportUpdate(
        alias = alias,
        currentVersion = currentVersion,
        availableVersion = latestVersion,
        type = updateType.name,
    )

    private fun CompatibilityIssue.toReportCompatibilityIssue(): ReportCompatibilityIssue =
        ReportCompatibilityIssue(
            ruleId = ruleId,
            severity = severity.name,
            message = message,
            suggestion = suggestion,
        )

    private fun VulnerabilityInfo.toReportVulnerability(): ReportVulnerability {
        val cveAlias = aliases.firstOrNull { it.startsWith("CVE-") }
        return ReportVulnerability(
            id = id,
            cveId = cveAlias,
            library = "$affectedCoordinate:$affectedVersion",
            severity = severity.name,
            cvssScore = cvssScore,
            fixedVersion = fixedVersion,
            summary = summary,
        )
    }

    private fun LicenseViolation.toReportLicense(): ReportLicense = ReportLicense(
        library = "$coordinate",
        licenseName = detectedLicense ?: "Unknown",
        status = violationType.name,
    )

    private fun VersionConflict.toReportConflict(): ReportConflict = ReportConflict(
        coordinate = this.coordinate.toString(),
        versions = requestedVersions,
    )

    private fun Constraint.toReportConstraint(): ReportConstraint = when (this) {
        is Constraint.VersionConstraintDef -> ReportConstraint(
            type = "version",
            description = "${coordinate}${because?.let { " ($it)" } ?: ""}",
        )

        is Constraint.Exclude              -> ReportConstraint(
            type = "exclude",
            description = "$coordinate",
        )

        is Constraint.Substitute           -> ReportConstraint(
            type = "substitute",
            description = "$from -> $to${toVersion?.let { ":$it" } ?: ""}${because?.let { " ($it)" } ?: ""}",
        )
    }
}
