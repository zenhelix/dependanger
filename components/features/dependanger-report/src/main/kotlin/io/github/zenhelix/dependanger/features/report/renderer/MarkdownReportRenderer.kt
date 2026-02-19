package io.github.zenhelix.dependanger.features.report.renderer

import io.github.zenhelix.dependanger.features.report.model.ReportBundle
import io.github.zenhelix.dependanger.features.report.model.ReportCompatibilityIssue
import io.github.zenhelix.dependanger.features.report.model.ReportConstraint
import io.github.zenhelix.dependanger.features.report.model.ReportData
import io.github.zenhelix.dependanger.features.report.model.ReportDeprecated
import io.github.zenhelix.dependanger.features.report.model.ReportLibrary
import io.github.zenhelix.dependanger.features.report.model.ReportLicense
import io.github.zenhelix.dependanger.features.report.model.ReportPlugin
import io.github.zenhelix.dependanger.features.report.model.ReportTransitives
import io.github.zenhelix.dependanger.features.report.model.ReportUpdate
import io.github.zenhelix.dependanger.features.report.model.ReportValidation
import io.github.zenhelix.dependanger.features.report.model.ReportVersion
import io.github.zenhelix.dependanger.features.report.model.ReportVulnerability

internal class MarkdownReportRenderer : ReportRenderer {

    override fun render(data: ReportData): String = buildString {
        appendLine("# Dependanger Report")
        appendLine()
        appendLine("Generated: ${data.generatedAt}")
        data.distribution?.let { appendLine("Distribution: $it") }
        appendLine()

        renderSummary(data)
        data.libraries?.let { renderLibraries(it) }
        data.plugins?.let { renderPlugins(it) }
        data.bundles?.let { renderBundles(it) }
        data.versions?.let { renderVersions(it) }
        data.deprecated?.let { renderDeprecated(it) }
        data.updates?.let { renderUpdates(it) }
        data.compatibility?.let { renderCompatibility(it) }
        data.vulnerabilities?.let { renderVulnerabilities(it) }
        data.licenses?.let { renderLicenses(it) }
        data.transitives?.let { renderTransitives(it) }
        data.constraints?.let { renderConstraints(it) }
        data.validation?.let { renderValidation(it) }
    }

    private fun StringBuilder.renderSummary(data: ReportData) {
        val summary = data.summary
        appendLine("## Summary")
        appendLine()
        appendLine("| Metric | Count |")
        appendLine("|--------|-------|")
        appendLine("| Libraries | ${summary.libraryCount} |")
        appendLine("| Plugins | ${summary.pluginCount} |")
        appendLine("| Bundles | ${summary.bundleCount} |")
        if (summary.deprecatedCount > 0) {
            appendLine("| Deprecated | ${summary.deprecatedCount} |")
        }
        if (summary.updatesAvailable > 0) {
            appendLine("| Updates Available | ${summary.updatesAvailable} |")
        }
        if (summary.vulnerabilitiesCount > 0) {
            appendLine("| Vulnerabilities | ${summary.vulnerabilitiesCount} |")
            appendLine("| Critical | ${summary.criticalVulnerabilities} |")
            appendLine("| High | ${summary.highVulnerabilities} |")
        }
        if (summary.deniedLicenses > 0) {
            appendLine("| Denied Licenses | ${summary.deniedLicenses} |")
        }
        if (summary.unknownLicenses > 0) {
            appendLine("| Unknown Licenses | ${summary.unknownLicenses} |")
        }
        if (summary.compatibilityIssues > 0) {
            appendLine("| Compatibility Issues | ${summary.compatibilityIssues} |")
        }
        if (summary.validationErrors > 0) {
            appendLine("| Validation Errors | ${summary.validationErrors} |")
        }
        if (summary.validationWarnings > 0) {
            appendLine("| Validation Warnings | ${summary.validationWarnings} |")
        }
    }

    private fun StringBuilder.renderLibraries(libraries: List<ReportLibrary>) {
        appendLine()
        appendLine("## Libraries (${libraries.size})")
        appendLine()
        appendLine("| Alias | Coordinates | Version | Tags |")
        appendLine("|-------|------------|---------|------|")
        for (lib in libraries) {
            val tags = lib.tags.joinToString(", ").ifEmpty { "-" }
            appendLine("| ${lib.alias.escapeCell()} | ${lib.group}:${lib.artifact} | ${lib.version?.escapeCell() ?: "N/A"} | ${tags.escapeCell()} |")
        }
    }

    private fun StringBuilder.renderPlugins(plugins: List<ReportPlugin>) {
        appendLine()
        appendLine("## Plugins (${plugins.size})")
        appendLine()
        appendLine("| Alias | Plugin ID | Version |")
        appendLine("|-------|-----------|---------|")
        for (p in plugins) {
            appendLine("| ${p.alias.escapeCell()} | ${p.id.escapeCell()} | ${p.version?.escapeCell() ?: "N/A"} |")
        }
    }

    private fun StringBuilder.renderBundles(bundles: List<ReportBundle>) {
        appendLine()
        appendLine("## Bundles (${bundles.size})")
        appendLine()
        for (b in bundles) {
            appendLine("- **${b.alias.escapeCell()}**: ${b.libraries.joinToString(", ") { it.escapeCell() }}")
        }
    }

    private fun StringBuilder.renderVersions(versions: List<ReportVersion>) {
        appendLine()
        appendLine("## Versions (${versions.size})")
        appendLine()
        appendLine("| Alias | Value | Fallback |")
        appendLine("|-------|-------|----------|")
        for (v in versions) {
            val fallback = if (v.hasFallback) "Yes" else "-"
            appendLine("| ${v.alias.escapeCell()} | ${v.value.escapeCell()} | $fallback |")
        }
    }

    private fun StringBuilder.renderDeprecated(deprecated: List<ReportDeprecated>) {
        appendLine()
        appendLine("## Deprecated Libraries (${deprecated.size})")
        appendLine()
        appendLine("| Library | Reason | Replacement |")
        appendLine("|---------|--------|-------------|")
        for (d in deprecated) {
            appendLine("| ${d.alias.escapeCell()} | ${d.reason?.escapeCell() ?: "-"} | ${d.replacement?.escapeCell() ?: "-"} |")
        }
    }

    private fun StringBuilder.renderUpdates(updates: List<ReportUpdate>) {
        appendLine()
        appendLine("## Available Updates (${updates.size})")
        appendLine()
        appendLine("| Library | Current | Available | Type |")
        appendLine("|---------|---------|-----------|------|")
        for (u in updates) {
            appendLine("| ${u.alias.escapeCell()} | ${u.currentVersion.escapeCell()} | ${u.availableVersion.escapeCell()} | ${u.type} |")
        }
    }

    private fun StringBuilder.renderCompatibility(issues: List<ReportCompatibilityIssue>) {
        appendLine()
        appendLine("## Compatibility Issues (${issues.size})")
        appendLine()
        appendLine("| Rule | Severity | Message | Suggestion |")
        appendLine("|------|----------|---------|------------|")
        for (issue in issues) {
            appendLine("| ${issue.ruleId.escapeCell()} | ${issue.severity} | ${issue.message.escapeCell()} | ${issue.suggestion?.escapeCell() ?: "-"} |")
        }
    }

    private fun StringBuilder.renderVulnerabilities(vulns: List<ReportVulnerability>) {
        appendLine()
        appendLine("## Vulnerabilities (${vulns.size})")
        appendLine()
        appendLine("| Library | CVE | Severity | CVSS | Fixed In | Summary |")
        appendLine("|---------|-----|----------|------|----------|---------|")
        for (v in vulns) {
            val cve = v.cveId ?: v.id
            val cvss = v.cvssScore?.let { "%.1f".format(it) } ?: "-"
            appendLine("| ${v.library.escapeCell()} | ${cve.escapeCell()} | ${v.severity} | $cvss | ${v.fixedVersion?.escapeCell() ?: "-"} | ${v.summary.escapeCell()} |")
        }
    }

    private fun StringBuilder.renderLicenses(licenses: List<ReportLicense>) {
        appendLine()
        appendLine("## Licenses (${licenses.size})")
        appendLine()
        appendLine("| Library | License | Status |")
        appendLine("|---------|---------|--------|")
        for (l in licenses) {
            appendLine("| ${l.library.escapeCell()} | ${l.licenseName.escapeCell()} | ${l.status} |")
        }
    }

    private fun StringBuilder.renderTransitives(transitives: ReportTransitives) {
        appendLine()
        appendLine("## Transitive Dependencies")
        appendLine()
        appendLine("- Direct: ${transitives.directCount}")
        appendLine("- Transitive: ${transitives.transitiveCount}")
        appendLine("- Conflicts: ${transitives.conflictCount}")
        if (transitives.conflicts.isNotEmpty()) {
            appendLine()
            appendLine("### Version Conflicts")
            appendLine()
            appendLine("| Artifact | Versions |")
            appendLine("|----------|----------|")
            for (c in transitives.conflicts) {
                appendLine("| ${c.artifact.escapeCell()} | ${c.versions.joinToString(" vs ").escapeCell()} |")
            }
        }
    }

    private fun StringBuilder.renderConstraints(constraints: List<ReportConstraint>) {
        appendLine()
        appendLine("## Constraints (${constraints.size})")
        appendLine()
        for (c in constraints) {
            appendLine("- **${c.type}**: ${c.description.escapeCell()}")
        }
    }

    private fun StringBuilder.renderValidation(validation: ReportValidation) {
        appendLine()
        appendLine("## Validation")
        appendLine()
        if (validation.errors.isNotEmpty()) {
            appendLine("### Errors (${validation.errors.size})")
            appendLine()
            for (e in validation.errors) {
                appendLine("- \u274C [${e.code.escapeCell()}] ${e.message.escapeCell()}")
            }
        }
        if (validation.warnings.isNotEmpty()) {
            appendLine("### Warnings (${validation.warnings.size})")
            appendLine()
            for (w in validation.warnings) {
                appendLine("- \u26A0\uFE0F [${w.code.escapeCell()}] ${w.message.escapeCell()}")
            }
        }
        if (validation.errors.isEmpty() && validation.warnings.isEmpty()) {
            appendLine("All checks passed.")
        }
    }

    private companion object {

        private fun String.escapeCell(): String =
            replace("|", "\\|").replace("\n", " ")
    }
}
