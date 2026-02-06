package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public sealed class FallbackCondition {
    @Serializable
    public data class JdkBelow(val version: Int) : FallbackCondition()

    @Serializable
    public data class JdkAtLeast(val version: Int) : FallbackCondition()

    @Serializable
    public data class JdkBetween(val min: Int, val max: Int) : FallbackCondition()

    @Serializable
    public data class KotlinVersionBelow(val version: String) : FallbackCondition()

    @Serializable
    public data class KotlinVersionAtLeast(val version: String) : FallbackCondition()

    @Serializable
    public data class GradleVersionBelow(val version: String) : FallbackCondition()

    @Serializable
    public data class GradleVersionAtLeast(val version: String) : FallbackCondition()

    @Serializable
    public data class DistributionCondition(val name: String) : FallbackCondition()

    @Serializable
    public data class EnvironmentCondition(val key: String, val value: String) : FallbackCondition()

    @Serializable
    public data class All(val conditions: List<FallbackCondition>) : FallbackCondition()

    @Serializable
    public data class Any(val conditions: List<FallbackCondition>) : FallbackCondition()

    @Serializable
    public data class Not(val condition: FallbackCondition) : FallbackCondition()
}
