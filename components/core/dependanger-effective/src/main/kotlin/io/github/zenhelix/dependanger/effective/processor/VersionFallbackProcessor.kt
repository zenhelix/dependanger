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
    override val order: Int = phase.order
    override val isOptional: Boolean = false
    override val description: String = "Applies version fallback rules based on environment conditions"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val originalVersions = context.originalMetadata.versions
        val versionsWithFallbacks = originalVersions.filter { it.fallbacks.isNotEmpty() }

        if (versionsWithFallbacks.isEmpty()) return metadata

        val (updatedVersions, diagnostics) = versionsWithFallbacks.fold(
            metadata.versions to metadata.diagnostics
        ) { (versions, diags), version ->
            val current = versions[version.name]
                ?: return@fold versions to diags

            val matchedFallback = version.fallbacks
                .firstOrNull { evaluateCondition(it.condition, context) }
                ?: return@fold versions to diags

            val newVersions = versions + (version.name to current.copy(value = matchedFallback.value))
            val newDiags = diags + Diagnostics.info(
                code = "FALLBACK_APPLIED",
                message = "Version '${version.name}': fallback '${matchedFallback.value}' applied " +
                        "(was '${current.value}', condition: ${matchedFallback.condition})",
                processorId = id,
                context = emptyMap(),
            )
            newVersions to newDiags
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
