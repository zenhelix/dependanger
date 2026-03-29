package io.github.zenhelix.dependanger.cli.sarif

import io.github.zenhelix.dependanger.cli.CliDefaults
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity

private const val TOOL_NAME: String = "dependanger"
private const val TOOL_INFO_URI: String = "https://github.com/zenhelix/dependanger"

public fun renderSarif(vulnerabilities: List<VulnerabilityInfo>): String {
    val rules = vulnerabilities.distinctBy { it.id }.map { vuln ->
        SarifRule(
            id = vuln.id,
            shortDescription = SarifMessage(text = vuln.summary),
            help = SarifMessage(text = buildHelpText(vuln)),
            properties = SarifRuleProperties(
                securitySeverity = formatSeverityScore(vuln)
            )
        )
    }

    val results = vulnerabilities.map { vuln ->
        SarifResult(
            ruleId = vuln.id,
            level = mapSeverityToLevel(vuln.severity),
            message = SarifMessage(
                text = "${vuln.summary} in ${vuln.affectedGroup}:${vuln.affectedArtifact}:${vuln.affectedVersion}"
            ),
            locations = listOf(
                SarifLocation(
                    physicalLocation = SarifPhysicalLocation(
                        artifactLocation = SarifArtifactLocation(uri = "metadata.json")
                    )
                )
            )
        )
    }

    val report = SarifReport(
        runs = listOf(
            SarifRun(
                tool = SarifTool(
                    driver = SarifDriver(
                        name = TOOL_NAME,
                        version = CliDefaults.TOOL_VERSION,
                        informationUri = TOOL_INFO_URI,
                        rules = rules
                    )
                ),
                results = results
            )
        )
    )

    return CliDefaults.CLI_JSON.encodeToString(SarifReport.serializer(), report)
}

private fun mapSeverityToLevel(severity: VulnerabilitySeverity): String = when (severity) {
    VulnerabilitySeverity.CRITICAL, VulnerabilitySeverity.HIGH -> "error"
    VulnerabilitySeverity.MEDIUM                               -> "warning"
    VulnerabilitySeverity.LOW,
    VulnerabilitySeverity.NONE,
    VulnerabilitySeverity.UNKNOWN                              -> "note"
}

private fun formatSeverityScore(vuln: VulnerabilityInfo): String =
    vuln.cvssScore?.toString() ?: defaultSeverityScore(vuln.severity)

private fun defaultSeverityScore(severity: VulnerabilitySeverity): String = when (severity) {
    VulnerabilitySeverity.CRITICAL -> "9.0"
    VulnerabilitySeverity.HIGH     -> "7.0"
    VulnerabilitySeverity.MEDIUM   -> "4.0"
    VulnerabilitySeverity.LOW      -> "2.0"
    VulnerabilitySeverity.NONE     -> "0.0"
    VulnerabilitySeverity.UNKNOWN  -> "0.0"
}

private fun buildHelpText(vuln: VulnerabilityInfo): String = buildString {
    append("Vulnerability ${vuln.id} affects ${vuln.affectedGroup}:${vuln.affectedArtifact}:${vuln.affectedVersion}.")
    vuln.fixedVersion?.let { append(" Fixed in version $it.") }
    vuln.url?.let { append(" Details: $it") }
}
