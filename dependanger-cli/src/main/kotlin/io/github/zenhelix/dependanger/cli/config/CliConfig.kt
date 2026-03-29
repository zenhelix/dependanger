package io.github.zenhelix.dependanger.cli.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class CliConfig(
    val defaults: DefaultsConfig = DefaultsConfig(),
    val generate: GenerateConfig = GenerateConfig(),
    @SerialName("check-updates")
    val checkUpdates: CheckUpdatesConfig = CheckUpdatesConfig(),
    val analyze: AnalyzeConfig = AnalyzeConfig(),
    @SerialName("security-check")
    val securityCheck: SecurityCheckConfig = SecurityCheckConfig(),
    @SerialName("license-check")
    val licenseCheck: LicenseCheckConfig = LicenseCheckConfig(),
)

@Serializable
public data class DefaultsConfig(
    val input: String = "./metadata.json",
    val output: String = "./build/dependanger",
)

@Serializable
public data class GenerateConfig(
    val toml: TomlGenerateConfig = TomlGenerateConfig(),
    val bom: BomGenerateConfig = BomGenerateConfig(),
)

@Serializable
public data class TomlGenerateConfig(
    val filename: String = "libs.versions.toml",
)

@Serializable
public data class BomGenerateConfig(
    @SerialName("group-id")
    val groupId: String? = null,
    @SerialName("artifact-id")
    val artifactId: String? = null,
)

@Serializable
public data class CheckUpdatesConfig(
    @SerialName("include-pre-release")
    val includePreRelease: Boolean = false,
    @SerialName("fail-on-major")
    val failOnMajor: Boolean = false,
)

@Serializable
public data class AnalyzeConfig(
    @SerialName("target-jdk")
    val targetJdk: Int = 17,
    @SerialName("fail-on-error")
    val failOnError: Boolean = false,
)

@Serializable
public data class SecurityCheckConfig(
    @SerialName("min-severity")
    val minSeverity: String = "high",
    @SerialName("fail-on")
    val failOn: String = "none",
)

@Serializable
public data class LicenseCheckConfig(
    @SerialName("fail-on-denied")
    val failOnDenied: Boolean = false,
    @SerialName("fail-on-unknown")
    val failOnUnknown: Boolean = false,
)
