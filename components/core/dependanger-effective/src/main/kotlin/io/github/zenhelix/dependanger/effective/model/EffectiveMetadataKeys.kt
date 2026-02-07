package io.github.zenhelix.dependanger.effective.model

public object EffectiveMetadataKeys {
    public val UPDATES: ExtensionKey<List<UpdateAvailableInfo>> = ExtensionKey("updates")
    public val VULNERABILITIES: ExtensionKey<List<VulnerabilityInfo>> = ExtensionKey("vulnerabilities")
    public val LICENSE_VIOLATIONS: ExtensionKey<List<LicenseViolation>> = ExtensionKey("licenseViolations")
    public val COMPATIBILITY_ISSUES: ExtensionKey<List<CompatibilityIssue>> = ExtensionKey("compatibilityIssues")
    public val TRANSITIVES: ExtensionKey<List<TransitiveTree>> = ExtensionKey("transitives")
    public val FLAT_DEPENDENCIES: ExtensionKey<List<FlatDependency>> = ExtensionKey("flatDependencies")
    public val VERSION_CONFLICTS: ExtensionKey<List<VersionConflict>> = ExtensionKey("versionConflicts")
}

public val EffectiveMetadata.updates: List<UpdateAvailableInfo>
    get() = getExtension(EffectiveMetadataKeys.UPDATES) ?: emptyList()

public val EffectiveMetadata.vulnerabilities: List<VulnerabilityInfo>
    get() = getExtension(EffectiveMetadataKeys.VULNERABILITIES) ?: emptyList()

public val EffectiveMetadata.licenseViolations: List<LicenseViolation>
    get() = getExtension(EffectiveMetadataKeys.LICENSE_VIOLATIONS) ?: emptyList()

public val EffectiveMetadata.compatibilityIssues: List<CompatibilityIssue>
    get() = getExtension(EffectiveMetadataKeys.COMPATIBILITY_ISSUES) ?: emptyList()

public val EffectiveMetadata.transitives: List<TransitiveTree>
    get() = getExtension(EffectiveMetadataKeys.TRANSITIVES) ?: emptyList()

public val EffectiveMetadata.flatDependencies: List<FlatDependency>
    get() = getExtension(EffectiveMetadataKeys.FLAT_DEPENDENCIES) ?: emptyList()

public val EffectiveMetadata.versionConflicts: List<VersionConflict>
    get() = getExtension(EffectiveMetadataKeys.VERSION_CONFLICTS) ?: emptyList()
