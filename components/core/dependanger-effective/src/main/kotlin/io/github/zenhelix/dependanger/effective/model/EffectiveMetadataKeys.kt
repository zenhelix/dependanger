package io.github.zenhelix.dependanger.effective.model

public object EffectiveMetadataKeys {
    public val COMPATIBILITY_ISSUES: ExtensionKey<List<CompatibilityIssue>> = ExtensionKey("compatibilityIssues")
}

public val EffectiveMetadata.compatibilityIssues: List<CompatibilityIssue>
    get() = getExtension(EffectiveMetadataKeys.COMPATIBILITY_ISSUES) ?: emptyList()
