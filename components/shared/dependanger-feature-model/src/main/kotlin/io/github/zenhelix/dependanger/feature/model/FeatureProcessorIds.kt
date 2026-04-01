package io.github.zenhelix.dependanger.feature.model

/**
 * Canonical identifiers for feature processors.
 * Used by feature modules, gradle plugin, and CLI to reference processors by ID
 * without requiring a direct dependency on the feature implementation module.
 */
public object FeatureProcessorIds {
    public const val UPDATE_CHECK: String = "update-check"
    public const val SECURITY_CHECK: String = "security-check"
    public const val LICENSE_CHECK: String = "license-check"
    public const val TRANSITIVE_RESOLVER: String = "transitive-resolver"
    public const val COMPATIBILITY_ANALYSIS: String = "compatibility-analysis"
}
