package io.github.zenhelix.dependanger.cli.config

public data class CliConfig(
    val defaults: DefaultsConfig = DefaultsConfig(),
    val generate: GenerateConfig = GenerateConfig(),
    val checkUpdates: CheckUpdatesConfig = CheckUpdatesConfig(),
    val analyze: AnalyzeConfig = AnalyzeConfig(),
    val securityCheck: SecurityCheckConfig = SecurityCheckConfig(),
    val licenseCheck: LicenseCheckConfig = LicenseCheckConfig(),
)

public data class DefaultsConfig(
    val input: String = "./metadata.json",
    val output: String = "./build/dependanger",
)

public data class GenerateConfig(
    val toml: TomlGenerateConfig = TomlGenerateConfig(),
    val bom: BomGenerateConfig = BomGenerateConfig(),
)

public data class TomlGenerateConfig(
    val filename: String = "libs.versions.toml",
)

public data class BomGenerateConfig(
    val groupId: String? = null,
    val artifactId: String? = null,
)

public data class CheckUpdatesConfig(
    val includePreRelease: Boolean = false,
    val failOnMajor: Boolean = false,
)

public data class AnalyzeConfig(
    val targetJdk: Int = 17,
    val failOnError: Boolean = false,
)

public data class SecurityCheckConfig(
    val minSeverity: String = "high",
    val failOn: String = "none",
)

public data class LicenseCheckConfig(
    val failOnDenied: Boolean = false,
    val failOnUnknown: Boolean = false,
)
