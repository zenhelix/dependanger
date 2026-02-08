package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class CompatibilityRule {
    public abstract val name: String
    public abstract val severity: Severity
    public abstract val message: String?

    @Serializable @SerialName("jdkRequirement")
    public data class JdkRequirement(
        override val name: String,
        val matches: String,
        val minJdk: Int? = null,
        val maxJdk: Int? = null,
        override val severity: Severity = Severity.ERROR,
        override val message: String? = null,
    ) : CompatibilityRule()

    @Serializable @SerialName("mutualExclusion")
    public data class MutualExclusion(
        override val name: String,
        val libraries: List<String>,
        override val severity: Severity = Severity.WARNING,
        override val message: String? = null,
    ) : CompatibilityRule()

    @Serializable @SerialName("versionConstraint")
    public data class VersionConstraint(
        override val name: String,
        val libraries: List<String>,
        val constraint: VersionConstraintType,
        override val severity: Severity = Severity.ERROR,
        override val message: String? = null,
    ) : CompatibilityRule()

    @Serializable @SerialName("customRule")
    public data class CustomRule(
        override val name: String,
        val ruleId: String,
        val parameters: Map<String, String> = emptyMap(),
        override val severity: Severity = Severity.WARNING,
        override val message: String? = null,
    ) : CompatibilityRule()
}
