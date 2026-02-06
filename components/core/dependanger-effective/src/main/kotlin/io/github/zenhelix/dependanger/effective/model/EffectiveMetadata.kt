package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public data class EffectiveMetadata(
    val schemaVersion: String = "1.0",
    val distribution: String? = null,
    val versions: Map<String, ResolvedVersion> = emptyMap(),
    val libraries: List<EffectiveLibrary> = emptyList(),
    val plugins: List<EffectivePlugin> = emptyList(),
    val bundles: List<EffectiveBundle> = emptyList(),
    val diagnostics: ProcessingDiagnostics = ProcessingDiagnostics(),
    val updates: List<UpdateAvailableInfo> = emptyList(),
    val vulnerabilities: List<VulnerabilityInfo> = emptyList(),
    val licenseViolations: List<LicenseViolation> = emptyList(),
    val compatibilityIssues: List<CompatibilityIssue> = emptyList(),
    val transitives: List<TransitiveTree> = emptyList(),
    val flatDependencies: List<FlatDependency> = emptyList(),
    val versionConflicts: List<VersionConflict> = emptyList(),
)
