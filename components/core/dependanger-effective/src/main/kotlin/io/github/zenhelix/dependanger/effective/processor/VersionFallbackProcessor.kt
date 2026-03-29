package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.FallbackCondition
import io.github.zenhelix.dependanger.core.util.VersionComparator
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class VersionFallbackProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.VERSION_FALLBACK
    override val phase: ProcessingPhase = ProcessingPhase.VERSION_FALLBACK
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.LIBRARY_FILTER))
    override val isOptional: Boolean = false
    override val description: String = "Applies version fallback rules based on environment conditions"
    override fun supports(context: ProcessingContext): Boolean = true

    private data class ConditionResult(
        val matched: Boolean,
        val diagnostics: Diagnostics,
    )

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

            var conditionDiags = Diagnostics.EMPTY
            val matchedFallback = version.fallbacks
                .firstOrNull { fallback ->
                    val result = evaluateCondition(fallback.condition, context, version.name)
                    conditionDiags = conditionDiags + result.diagnostics
                    result.matched
                }

            val newDiags = diags + conditionDiags
            if (matchedFallback == null) return@fold versions to newDiags

            val newVersions = versions + (version.name to current.copy(value = matchedFallback.value))
            val appliedDiag = Diagnostics.info(
                code = DiagnosticCodes.Version.FALLBACK_APPLIED,
                message = "Version '${version.name}': fallback '${matchedFallback.value}' applied (was '${current.value}', condition: ${matchedFallback.condition})",
                processorId = id,
                context = emptyMap(),
            )
            newVersions to (newDiags + appliedDiag)
        }

        return metadata.copy(versions = updatedVersions, diagnostics = diagnostics)
    }

    private fun evaluateCondition(
        condition: FallbackCondition,
        context: ProcessingContext,
        versionAlias: String,
    ): ConditionResult {
        val env = context.environment
        return when (condition) {
            is FallbackCondition.JdkBelow              ->
                checkEnvField(env.jdkVersion, "jdkVersion", versionAlias) { jdk ->
                    jdk < condition.version
                }

            is FallbackCondition.JdkAtLeast            ->
                checkEnvField(env.jdkVersion, "jdkVersion", versionAlias) { jdk ->
                    jdk >= condition.version
                }

            is FallbackCondition.JdkBetween            ->
                checkEnvField(env.jdkVersion, "jdkVersion", versionAlias) { jdk ->
                    jdk in condition.min..condition.max
                }

            is FallbackCondition.KotlinVersionBelow    ->
                checkEnvField(env.kotlinVersion, "kotlinVersion", versionAlias) { kv ->
                    VersionComparator.compare(kv, condition.version) < 0
                }

            is FallbackCondition.KotlinVersionAtLeast  ->
                checkEnvField(env.kotlinVersion, "kotlinVersion", versionAlias) { kv ->
                    VersionComparator.compare(kv, condition.version) >= 0
                }

            is FallbackCondition.GradleVersionBelow    ->
                checkEnvField(env.gradleVersion, "gradleVersion", versionAlias) { gv ->
                    VersionComparator.compare(gv, condition.version) < 0
                }

            is FallbackCondition.GradleVersionAtLeast  ->
                checkEnvField(env.gradleVersion, "gradleVersion", versionAlias) { gv ->
                    VersionComparator.compare(gv, condition.version) >= 0
                }

            is FallbackCondition.DistributionCondition ->
                ConditionResult(context.activeDistribution == condition.name, Diagnostics.EMPTY)

            is FallbackCondition.EnvironmentCondition  ->
                ConditionResult(env.environmentVariables[condition.key] == condition.value, Diagnostics.EMPTY)

            is FallbackCondition.All                   -> {
                val results = condition.conditions.map { evaluateCondition(it, context, versionAlias) }
                val combinedDiags = results.fold(Diagnostics.EMPTY) { acc, r -> acc + r.diagnostics }
                ConditionResult(results.all { it.matched }, combinedDiags)
            }

            is FallbackCondition.Any                   -> {
                val results = condition.conditions.map { evaluateCondition(it, context, versionAlias) }
                val combinedDiags = results.fold(Diagnostics.EMPTY) { acc, r -> acc + r.diagnostics }
                ConditionResult(results.any { it.matched }, combinedDiags)
            }

            is FallbackCondition.Not                   -> {
                val inner = evaluateCondition(condition.condition, context, versionAlias)
                ConditionResult(!inner.matched, inner.diagnostics)
            }
        }
    }

    private fun <T : Any> checkEnvField(
        fieldValue: T?,
        fieldName: String,
        versionAlias: String,
        evaluate: (T) -> Boolean,
    ): ConditionResult {
        if (fieldValue == null) {
            val diagnostic = Diagnostics.warning(
                code = DiagnosticCodes.Version.FALLBACK_ENV_MISSING,
                message = "Environment field '$fieldName' is null, cannot evaluate fallback condition for version '$versionAlias'",
                processorId = id,
                context = emptyMap(),
            )
            return ConditionResult(matched = false, diagnostics = diagnostic)
        }
        return ConditionResult(matched = evaluate(fieldValue), diagnostics = Diagnostics.EMPTY)
    }
}
