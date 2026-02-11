package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.FallbackCondition
import io.github.zenhelix.dependanger.core.model.Version
import io.github.zenhelix.dependanger.core.model.VersionFallback

@DependangerDslMarker
public class VersionsDsl {
    private val _versions: MutableList<Version> = mutableListOf()
    public val versions: List<Version> get() = _versions.toList()

    public fun version(alias: String, value: String) {
        _versions.add(Version(name = alias, value = value, fallbacks = emptyList()))
    }

    public fun version(alias: String, value: String, block: VersionDsl.() -> Unit) {
        val dsl = VersionDsl().apply(block)
        _versions.add(Version(name = alias, value = value, fallbacks = dsl.fallbacks))
    }
}

@DependangerDslMarker
public class VersionDsl {
    private val _fallbacks: MutableList<VersionFallback> = mutableListOf()
    public val fallbacks: List<VersionFallback> get() = _fallbacks.toList()

    public fun fallback(value: String, condition: FallbackConditionDsl.() -> FallbackCondition) {
        val cond = FallbackConditionDsl().condition()
        _fallbacks.add(VersionFallback(value = value, condition = cond))
    }
}

@DependangerDslMarker
public class FallbackConditionDsl {
    public fun jdkBelow(version: Int): FallbackCondition = FallbackCondition.JdkBelow(version)
    public fun jdkAtLeast(version: Int): FallbackCondition = FallbackCondition.JdkAtLeast(version)
    public fun jdkBetween(min: Int, max: Int): FallbackCondition = FallbackCondition.JdkBetween(min, max)
    public fun kotlinVersionBelow(version: String): FallbackCondition = FallbackCondition.KotlinVersionBelow(version)
    public fun kotlinVersionAtLeast(version: String): FallbackCondition = FallbackCondition.KotlinVersionAtLeast(version)
    public fun gradleVersionBelow(version: String): FallbackCondition = FallbackCondition.GradleVersionBelow(version)
    public fun gradleVersionAtLeast(version: String): FallbackCondition = FallbackCondition.GradleVersionAtLeast(version)
    public fun distribution(name: String): FallbackCondition = FallbackCondition.DistributionCondition(name)
    public fun environment(key: String, value: String): FallbackCondition = FallbackCondition.EnvironmentCondition(key, value)
    public fun all(vararg conditions: FallbackCondition): FallbackCondition = FallbackCondition.All(conditions.toList())
    public fun any(vararg conditions: FallbackCondition): FallbackCondition = FallbackCondition.Any(conditions.toList())
    public fun not(condition: FallbackCondition): FallbackCondition = FallbackCondition.Not(condition)
}
