package io.github.zenhelix.dependanger.effective.model

public object EffectiveMetadataKeys {
    public val COMPATIBILITY_ISSUES: ExtensionKey<List<CompatibilityIssue>> = ExtensionKey("compatibilityIssues")

    // TODO: добавляются при реализации feature-модулей (см. specs/)
    // UPDATES: ExtensionKey<List<UpdateAvailableInfo>>            — SPEC-07
    // VULNERABILITIES: ExtensionKey<List<VulnerabilityInfo>>      — SPEC-10
    // LICENSE_VIOLATIONS: ExtensionKey<List<LicenseViolation>>    — SPEC-11
    // TRANSITIVES: ExtensionKey<List<DependencyTree>>             — SPEC-12
    // FLAT_DEPENDENCIES: ExtensionKey<List<FlatDependency>>       — SPEC-12
    // VERSION_CONFLICTS: ExtensionKey<List<VersionConflict>>      — SPEC-12
}

public val EffectiveMetadata.compatibilityIssues: List<CompatibilityIssue>
    get() = getExtension(EffectiveMetadataKeys.COMPATIBILITY_ISSUES) ?: emptyList()
