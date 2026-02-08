package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class FallbackCondition {
    @Serializable @SerialName("jdkBelow")
    public data class JdkBelow(val version: Int) : FallbackCondition()

    @Serializable @SerialName("jdkAtLeast")
    public data class JdkAtLeast(val version: Int) : FallbackCondition()

    @Serializable @SerialName("jdkBetween")
    public data class JdkBetween(val min: Int, val max: Int) : FallbackCondition()

    @Serializable @SerialName("kotlinVersionBelow")
    public data class KotlinVersionBelow(val version: String) : FallbackCondition()

    @Serializable @SerialName("kotlinVersionAtLeast")
    public data class KotlinVersionAtLeast(val version: String) : FallbackCondition()

    @Serializable @SerialName("gradleVersionBelow")
    public data class GradleVersionBelow(val version: String) : FallbackCondition()

    @Serializable @SerialName("gradleVersionAtLeast")
    public data class GradleVersionAtLeast(val version: String) : FallbackCondition()

    @Serializable @SerialName("distribution")
    public data class DistributionCondition(val name: String) : FallbackCondition()

    @Serializable @SerialName("environment")
    public data class EnvironmentCondition(val key: String, val value: String) : FallbackCondition()

    @Serializable @SerialName("all")
    public data class All(val conditions: List<FallbackCondition>) : FallbackCondition()

    @Serializable @SerialName("any")
    public data class Any(val conditions: List<FallbackCondition>) : FallbackCondition()

    @Serializable @SerialName("not")
    public data class Not(val condition: FallbackCondition) : FallbackCondition()
}
