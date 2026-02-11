package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.CompatibilityRule
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.VersionConstraintType
import io.github.zenhelix.dependanger.core.util.GlobMatcher
import io.github.zenhelix.dependanger.core.util.VersionComparator
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssuesExtensionKey
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.compatibilityIssues
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class CompatRulesProcessor : EffectiveMetadataProcessor {
    override val id: String = "compat-rules"
    override val phase: ProcessingPhase = ProcessingPhase.COMPAT_RULES
    override val order: Int = phase.order
    override val isOptional: Boolean = false
    override val description: String = "Checks compatibility rules between libraries"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val rules = context.originalMetadata.compatibility
        if (rules.isEmpty()) return metadata

        val (issues, rulesDiagnostics) = rules.fold(
            emptyList<CompatibilityIssue>() to metadata.diagnostics
        ) { (accIssues, accDiag), rule ->
            when (rule) {
                is CompatibilityRule.JdkRequirement    ->
                    (accIssues + checkJdkRequirement(rule, metadata, context.environment)) to accDiag

                is CompatibilityRule.MutualExclusion   ->
                    (accIssues + checkMutualExclusion(rule, metadata)) to accDiag

                is CompatibilityRule.VersionConstraint -> (accIssues + checkVersionConstraint(rule, metadata)) to accDiag

                is CompatibilityRule.CustomRule        ->
                    accIssues to (accDiag + Diagnostics.info(
                        code = "COMPAT_CUSTOM_RULE_DEFERRED",
                        message = "Custom rule '${rule.ruleId}' deferred to compatibility-analysis processor",
                        processorId = id,
                        context = emptyMap(),
                    ))
            }
        }

        val allIssues = metadata.compatibilityIssues + issues

        val result = metadata.withExtension(
            CompatibilityIssuesExtensionKey,
            allIssues,
        )

        val issueDiagnostics = issues.fold(rulesDiagnostics) { accDiag, issue ->
            accDiag + when (issue.severity) {
                Severity.ERROR   -> Diagnostics.error(
                    code = "COMPAT_${issue.ruleId.uppercase()}",
                    message = issue.message,
                    processorId = id,
                    context = emptyMap(),
                )

                Severity.WARNING -> Diagnostics.warning(
                    code = "COMPAT_${issue.ruleId.uppercase()}",
                    message = issue.message,
                    processorId = id,
                    context = emptyMap(),
                )

                Severity.INFO    -> Diagnostics.info(
                    code = "COMPAT_${issue.ruleId.uppercase()}",
                    message = issue.message,
                    processorId = id,
                    context = emptyMap(),
                )
            }
        }

        return result.copy(diagnostics = issueDiagnostics)
    }

    private fun checkJdkRequirement(
        rule: CompatibilityRule.JdkRequirement,
        metadata: EffectiveMetadata,
        environment: ProcessingEnvironment,
    ): List<CompatibilityIssue> {
        val jdkVersion = environment.jdkVersion ?: return emptyList()
        val matchingLibs = metadata.libraries.filter { (alias, lib) ->
            GlobMatcher.matches(rule.matches, lib.group, lib.artifact) || GlobMatcher.matchesGlob(rule.matches, alias)
        }

        val minJdk = rule.minJdk
        val maxJdk = rule.maxJdk
        return matchingLibs.mapNotNull { (alias, _) ->
            val violatesMin = minJdk != null && jdkVersion < minJdk
            val violatesMax = maxJdk != null && jdkVersion > maxJdk
            if (violatesMin || violatesMax) {
                CompatibilityIssue(
                    ruleId = rule.name,
                    message = rule.message
                        ?: "Library '$alias' requires JDK ${rule.minJdk ?: "?"}-${rule.maxJdk ?: "?"}, current: $jdkVersion",
                    severity = rule.severity,
                    affectedLibraries = listOf(alias),
                    suggestion = "Upgrade JDK or use a compatible library version",
                )
            } else {
                null
            }
        }
    }

    private fun checkMutualExclusion(
        rule: CompatibilityRule.MutualExclusion,
        metadata: EffectiveMetadata,
    ): List<CompatibilityIssue> {
        val presentLibs = rule.libraries.filter { it in metadata.libraries }
        return if (presentLibs.size > 1) {
            listOf(
                CompatibilityIssue(
                    ruleId = rule.name,
                    message = rule.message
                        ?: "Mutually exclusive libraries present: ${presentLibs.joinToString()}",
                    severity = rule.severity,
                    affectedLibraries = presentLibs,
                    suggestion = "Remove one of the conflicting libraries",
                )
            )
        } else emptyList()
    }

    private fun checkVersionConstraint(
        rule: CompatibilityRule.VersionConstraint,
        metadata: EffectiveMetadata,
    ): List<CompatibilityIssue> {
        val libraryVersions = rule.libraries.mapNotNull { alias ->
            metadata.libraries[alias]?.version?.value?.let { alias to it }
        }

        if (libraryVersions.size < 2) return emptyList()

        val versions = libraryVersions.map { it.second }
        val violation = when (rule.constraint) {
            VersionConstraintType.SAME_VERSION     -> versions.distinct().size > 1
            VersionConstraintType.SAME_MAJOR       -> versions.map { VersionComparator.parseMajor(it) }.distinct().size > 1
            VersionConstraintType.SAME_MAJOR_MINOR -> versions.map { VersionComparator.parseMajorMinor(it) }.distinct().size > 1
        }

        return if (violation) {
            val details = libraryVersions.joinToString { "${it.first}=${it.second}" }
            listOf(
                CompatibilityIssue(
                    ruleId = rule.name,
                    message = rule.message
                        ?: "Version constraint ${rule.constraint} violated: $details",
                    severity = rule.severity,
                    affectedLibraries = libraryVersions.map { it.first },
                    suggestion = "Align versions according to ${rule.constraint} constraint",
                )
            )
        } else emptyList()
    }
}
