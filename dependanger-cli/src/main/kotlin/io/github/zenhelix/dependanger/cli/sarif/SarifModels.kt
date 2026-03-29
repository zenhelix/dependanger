package io.github.zenhelix.dependanger.cli.sarif

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class SarifReport(
    @SerialName("\$schema")
    val schema: String = "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json",
    val version: String = "2.1.0",
    val runs: List<SarifRun>,
)

@Serializable
public data class SarifRun(
    val tool: SarifTool,
    val results: List<SarifResult>,
)

@Serializable
public data class SarifTool(
    val driver: SarifDriver,
)

@Serializable
public data class SarifDriver(
    val name: String,
    val version: String,
    val informationUri: String,
    val rules: List<SarifRule>,
)

@Serializable
public data class SarifRule(
    val id: String,
    val shortDescription: SarifMessage,
    val help: SarifMessage,
    val properties: SarifRuleProperties,
)

@Serializable
public data class SarifRuleProperties(
    @SerialName("security-severity")
    val securitySeverity: String,
)

@Serializable
public data class SarifResult(
    val ruleId: String,
    val level: String,
    val message: SarifMessage,
    val locations: List<SarifLocation>,
)

@Serializable
public data class SarifMessage(
    val text: String,
)

@Serializable
public data class SarifLocation(
    val physicalLocation: SarifPhysicalLocation,
)

@Serializable
public data class SarifPhysicalLocation(
    val artifactLocation: SarifArtifactLocation,
)

@Serializable
public data class SarifArtifactLocation(
    val uri: String,
)
