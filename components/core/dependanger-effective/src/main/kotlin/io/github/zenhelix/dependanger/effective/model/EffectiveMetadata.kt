package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.Diagnostics
import kotlinx.serialization.Serializable

@Serializable
public data class EffectiveMetadata(
    val schemaVersion: String = "1.0",
    val distribution: String? = null,
    val versions: Map<String, ResolvedVersion> = emptyMap(),
    val libraries: Map<String, EffectiveLibrary> = emptyMap(),
    val plugins: Map<String, EffectivePlugin> = emptyMap(),
    val bundles: Map<String, EffectiveBundle> = emptyMap(),
    val diagnostics: Diagnostics = Diagnostics(),
    val processingInfo: ProcessingInfo? = null,
    val updates: List<UpdateAvailableInfo> = emptyList(),
    val vulnerabilities: List<VulnerabilityInfo> = emptyList(),
    val licenseViolations: List<LicenseViolation> = emptyList(),
    val compatibilityIssues: List<CompatibilityIssue> = emptyList(),
    val transitives: List<TransitiveTree> = emptyList(),
    val flatDependencies: List<FlatDependency> = emptyList(),
    val versionConflicts: List<VersionConflict> = emptyList(),
)
