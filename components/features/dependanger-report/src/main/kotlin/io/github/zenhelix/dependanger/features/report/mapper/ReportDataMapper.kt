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
        val allUpdates = effective.updates
        val allVulnerabilities = effective.vulnerabilities
        val allCompatIssues = effective.compatibilityIssues
        val allLicenseViolations = effective.licenseViolations

        val libraries = if (!summaryOnly && ReportSection.LIBRARIES in sections) {
            effective.libraries.values.map { it.toReportLibrary() }
        } else {
            null
        }

        val plugins = if (!summaryOnly && ReportSection.PLUGINS in sections) {
            effective.plugins.values.map { it.toReportPlugin() }
        } else {
            null
        }

        val bundles = if (!summaryOnly && ReportSection.BUNDLES in sections) {
            effective.bundles.values.map { ReportBundle(alias = it.alias, libraries = it.libraries) }
        } else {
            null
        }

        val versions = if (!summaryOnly && ReportSection.VERSIONS in sections) {
            buildVersions(effective, originalMetadata)
        } else {
            null
        }

        val updates = if (!summaryOnly && ReportSection.UPDATES in sections) {
            allUpdates.ifEmpty { null }?.map { it.toReportUpdate() }
        } else {
            null
        }

        val compatibility = if (!summaryOnly && ReportSection.COMPATIBILITY in sections) {
            allCompatIssues.ifEmpty { null }?.map { it.toReportCompatibilityIssue() }
        } else {
            null
        }

        val vulnerabilities = if (!summaryOnly && ReportSection.VULNERABILITIES in sections) {
            allVulnerabilities.ifEmpty { null }?.map { it.toReportVulnerability() }
        } else {
            null
        }

        val deprecated = if (!summaryOnly && ReportSection.DEPRECATED in sections) {
            effective.libraries.values
                .filter { it.isDeprecated }
                .map { it.toReportDeprecated() }
                .ifEmpty { null }
        } else {
            null
        }

        val licenses = if (!summaryOnly && ReportSection.LICENSES in sections) {
            allLicenseViolations.ifEmpty { null }?.map { it.toReportLicense() }
        } else {
            null
        }

        val transitives = if (!summaryOnly && ReportSection.TRANSITIVES in sections) {
            buildTransitives(effective)
        } else {
            null
        }

        val constraints = if (!summaryOnly && ReportSection.CONSTRAINTS in sections && originalMetadata != null) {
            originalMetadata.constraints
                .map { it.toReportConstraint() }
                .ifEmpty { null }
        } else {
            null
        }

        val validation = if (!summaryOnly && ReportSection.VALIDATION in sections) {
            buildValidation(effective)
        } else {
            null
        }

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
            libraries = libraries,
            plugins = plugins,
            bundles = bundles,
            versions = versions,
            updates = updates,
            compatibility = compatibility,
            vulnerabilities = vulnerabilities,
            deprecated = deprecated,
            licenses = licenses,
            transitives = transitives,
            constraints = constraints,
            validation = validation,
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
        val transitiveCount = flatDeps.count { !it.isDirectDependency }

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
        group = group,
        artifact = artifact,
        version = version.valueOrNull,
        tags = tags,
        isDeprecated = isDeprecated,
        isPlatform = isPlatform,
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
            library = "$affectedGroup:$affectedArtifact:$affectedVersion",
            severity = severity.name,
            cvssScore = cvssScore,
            fixedVersion = fixedVersion,
            summary = summary,
        )
    }

    private fun LicenseViolation.toReportLicense(): ReportLicense = ReportLicense(
        library = "$group:$artifact",
        licenseName = detectedLicense ?: "Unknown",
        status = violationType.name,
    )

    private fun VersionConflict.toReportConflict(): ReportConflict = ReportConflict(
        artifact = "$group:$artifact",
        versions = requestedVersions,
    )

    private fun Constraint.toReportConstraint(): ReportConstraint = when (this) {
        is Constraint.VersionConstraintDef -> ReportConstraint(
            type = "version",
            description = "${coordinates}${because?.let { " ($it)" } ?: ""}",
        )

        is Constraint.Exclude              -> ReportConstraint(
            type = "exclude",
            description = "$group:$artifact",
        )

        is Constraint.Substitute           -> ReportConstraint(
            type = "substitute",
            description = "$from -> $to${because?.let { " ($it)" } ?: ""}",
        )
    }
}
