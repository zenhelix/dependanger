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

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val rules = context.originalMetadata.compatibility
        if (rules.isEmpty()) return metadata

        val issues = mutableListOf<CompatibilityIssue>()
        var diagnostics = metadata.diagnostics

        for (rule in rules) {
            val ruleIssues = when (rule) {
                is CompatibilityRule.JdkRequirement    ->
                    checkJdkRequirement(rule, metadata, context.environment)

                is CompatibilityRule.MutualExclusion   ->
                    checkMutualExclusion(rule, metadata)

                is CompatibilityRule.VersionConstraint ->
                    checkVersionConstraint(rule, metadata)

                is CompatibilityRule.CustomRule        -> {
                    diagnostics = diagnostics + Diagnostics.info(
                        code = "COMPAT_CUSTOM_RULE_DEFERRED",
                        message = "Custom rule '${rule.ruleId}' deferred to compatibility-analysis processor",
                        processorId = id,
                    )
                    emptyList()
                }
            }
            issues.addAll(ruleIssues)
        }

        val existingIssues = metadata.compatibilityIssues
        val allIssues = existingIssues + issues

        var result = metadata.withExtension(
            CompatibilityIssuesExtensionKey,
            allIssues,
        )

        for (issue in issues) {
            diagnostics = diagnostics + when (issue.severity) {
                Severity.ERROR   -> Diagnostics.error(
                    code = "COMPAT_${issue.ruleId.uppercase()}",
                    message = issue.message,
                    processorId = id,
                )

                Severity.WARNING -> Diagnostics.warning(
                    code = "COMPAT_${issue.ruleId.uppercase()}",
                    message = issue.message,
                    processorId = id,
                )

                Severity.INFO    -> Diagnostics.info(
                    code = "COMPAT_${issue.ruleId.uppercase()}",
                    message = issue.message,
                    processorId = id,
                )
            }
        }

        return result.copy(diagnostics = diagnostics)
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

        val issues = mutableListOf<CompatibilityIssue>()
        val minJdk = rule.minJdk
        val maxJdk = rule.maxJdk
        for ((alias, _) in matchingLibs) {
            val violatesMin = minJdk != null && jdkVersion < minJdk
            val violatesMax = maxJdk != null && jdkVersion > maxJdk
            if (violatesMin || violatesMax) {
                issues.add(
                    CompatibilityIssue(
                        ruleId = rule.name,
                        message = rule.message
                            ?: "Library '$alias' requires JDK ${rule.minJdk ?: "?"}-${rule.maxJdk ?: "?"}, current: $jdkVersion",
                        severity = rule.severity,
                        affectedLibraries = listOf(alias),
                        suggestion = "Upgrade JDK or use a compatible library version",
                    )
                )
            }
        }
        return issues
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
