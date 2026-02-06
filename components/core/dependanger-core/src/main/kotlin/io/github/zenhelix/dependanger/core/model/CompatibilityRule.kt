package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public sealed class CompatibilityRule {
    public abstract val name: String
    public abstract val severity: Severity
    public abstract val message: String?

    @Serializable
    public data class JdkRequirement(
        override val name: String,
        val matches: String,
        val minJdk: Int? = null,
        val maxJdk: Int? = null,
        override val severity: Severity = Severity.ERROR,
        override val message: String? = null,
    ) : CompatibilityRule()

    @Serializable
    public data class MutualExclusion(
        override val name: String,
        val libraries: List<String>,
        override val severity: Severity = Severity.WARNING,
        override val message: String? = null,
    ) : CompatibilityRule()

    @Serializable
    public data class VersionConstraint(
        override val name: String,
        val libraries: List<String>,
        val constraint: VersionConstraintType,
        override val severity: Severity = Severity.ERROR,
        override val message: String? = null,
    ) : CompatibilityRule()

    @Serializable
    public data class CustomRule(
        override val name: String,
        override val severity: Severity = Severity.WARNING,
        override val message: String? = null,
    ) : CompatibilityRule()
}
