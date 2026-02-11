package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.FallbackCondition
import io.github.zenhelix.dependanger.core.util.VersionComparator
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class VersionFallbackProcessor : EffectiveMetadataProcessor {
    override val id: String = "version-fallback"
    override val phase: ProcessingPhase = ProcessingPhase.VERSION_FALLBACK

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val originalVersions = context.originalMetadata.versions
        val versionsWithFallbacks = originalVersions.filter { it.fallbacks.isNotEmpty() }

        if (versionsWithFallbacks.isEmpty()) return metadata

        val updatedVersions = metadata.versions.toMutableMap()
        var diagnostics = metadata.diagnostics

        for (version in versionsWithFallbacks) {
            val current = updatedVersions[version.name] ?: continue

            for (fallback in version.fallbacks) {
                if (evaluateCondition(fallback.condition, context)) {
                    updatedVersions[version.name] = current.copy(value = fallback.value)
                    diagnostics = diagnostics + Diagnostics.info(
                        code = "FALLBACK_APPLIED",
                        message = "Version '${version.name}': fallback '${fallback.value}' applied " +
                                "(was '${current.value}', condition: ${fallback.condition})",
                        processorId = id,
                    )
                    break
                }
            }
        }

        return metadata.copy(versions = updatedVersions, diagnostics = diagnostics)
    }

    private fun evaluateCondition(
        condition: FallbackCondition,
        context: ProcessingContext,
    ): Boolean {
        val env = context.environment
        return when (condition) {
            is FallbackCondition.JdkBelow              ->
                env.jdkVersion != null && env.jdkVersion < condition.version

            is FallbackCondition.JdkAtLeast            ->
                env.jdkVersion != null && env.jdkVersion >= condition.version

            is FallbackCondition.JdkBetween            ->
                env.jdkVersion != null && env.jdkVersion in condition.min..condition.max

            is FallbackCondition.KotlinVersionBelow    ->
                env.kotlinVersion != null && VersionComparator.compare(env.kotlinVersion, condition.version) < 0

            is FallbackCondition.KotlinVersionAtLeast  ->
                env.kotlinVersion != null && VersionComparator.compare(env.kotlinVersion, condition.version) >= 0

            is FallbackCondition.GradleVersionBelow    ->
                env.gradleVersion != null && VersionComparator.compare(env.gradleVersion, condition.version) < 0

            is FallbackCondition.GradleVersionAtLeast  ->
                env.gradleVersion != null && VersionComparator.compare(env.gradleVersion, condition.version) >= 0

            is FallbackCondition.DistributionCondition ->
                context.activeDistribution == condition.name

            is FallbackCondition.EnvironmentCondition  ->
                env.environmentVariables[condition.key] == condition.value

            is FallbackCondition.All                   ->
                condition.conditions.all { evaluateCondition(it, context) }

            is FallbackCondition.Any                   ->
                condition.conditions.any { evaluateCondition(it, context) }

            is FallbackCondition.Not                   ->
                !evaluateCondition(condition.condition, context)
        }
    }
}
