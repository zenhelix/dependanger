package io.github.zenhelix.dependanger.features.analysis

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.CompatibilityRule
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssuesExtensionKey
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.compatibilityIssues
import io.github.zenhelix.dependanger.effective.model.toDiagnostics
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ExecutionMode
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.effective.spi.CustomRuleHandler
import io.github.zenhelix.dependanger.effective.spi.CustomRuleHandlersKey
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds

private val logger = KotlinLogging.logger {}

public class CompatibilityCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = PROCESSOR_ID
    override val phase: ProcessingPhase = PHASE
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER))
    override val isOptional: Boolean = true
    override val description: String = "Performs advanced compatibility analysis between libraries"

    public companion object {
        public const val PROCESSOR_ID: String = FeatureProcessorIds.COMPATIBILITY_ANALYSIS
        public val PHASE: ProcessingPhase = ProcessingPhase("COMPATIBILITY_ANALYSIS", ExecutionMode.SEQUENTIAL)
    }
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val rules = context.originalMetadata.compatibility
        val customRules = rules.filterIsInstance<CompatibilityRule.CustomRule>()

        if (customRules.isEmpty()) {
            logger.info { "No custom compatibility rules to process" }
            return metadata.copy(
                diagnostics = metadata.diagnostics + Diagnostics.info(
                    code = DiagnosticCodes.Compatibility.NO_CUSTOM_RULES,
                    message = "No custom compatibility rules to process",
                    processorId = id,
                    context = emptyMap(),
                )
            )
        }

        val handlers: Map<String, CustomRuleHandler> = context[CustomRuleHandlersKey] ?: emptyMap()

        logger.debug { "Loaded ${handlers.size} custom rule handlers: ${handlers.keys}" }

        if (handlers.isEmpty()) {
            logger.info { "No custom rule handlers registered — skipping ${customRules.size} custom rule(s)" }
            return metadata.copy(
                diagnostics = metadata.diagnostics + Diagnostics.info(
                    code = DiagnosticCodes.Compatibility.NO_CUSTOM_HANDLERS,
                    message = "No custom rule handlers registered — ${customRules.size} custom rule(s) skipped. " +
                            "Register CustomRuleHandler implementations via META-INF/services to evaluate custom rules.",
                    processorId = id,
                    context = mapOf("skippedRules" to customRules.map { it.ruleId }.joinToString()),
                )
            )
        }

        val (newIssues, rulesDiagnostics) = customRules.fold(
            emptyList<CompatibilityIssue>() to metadata.diagnostics
        ) { (accIssues, accDiag), rule ->
            val handler = handlers[rule.ruleId]
            if (handler == null) {
                logger.warn { "Custom rule handler not found for '${rule.ruleId}'. Available handlers: ${handlers.keys}" }
                accIssues to (accDiag + Diagnostics.warning(
                    code = DiagnosticCodes.Compatibility.CUSTOM_HANDLER_NOT_FOUND,
                    message = "Custom rule handler not found for '${rule.ruleId}'. Available handlers: ${handlers.keys.joinToString()}",
                    processorId = id,
                    context = mapOf(
                        "ruleId" to rule.ruleId,
                        "availableHandlers" to handlers.keys.joinToString(),
                    ),
                ))
            } else {
                evaluateRule(handler, rule, metadata, context, accIssues, accDiag)
            }
        }

        return metadata
            .withExtension(CompatibilityIssuesExtensionKey, metadata.compatibilityIssues + newIssues)
            .copy(diagnostics = rulesDiagnostics)
    }

    private fun evaluateRule(
        handler: CustomRuleHandler,
        rule: CompatibilityRule.CustomRule,
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        accIssues: List<CompatibilityIssue>,
        accDiag: Diagnostics,
    ): Pair<List<CompatibilityIssue>, Diagnostics> = try {
        val issues = handler.evaluate(rule, metadata.libraries, context)
        val issueDiagnostics = issues.fold(accDiag) { diagAcc, issue ->
            diagAcc + issue.toDiagnostics(
                code = DiagnosticCodes.Compatibility.CUSTOM_RULE,
                processorId = id,
                message = "[${rule.ruleId}] ${issue.message}",
                context = mapOf(
                    "ruleId" to rule.ruleId,
                    "ruleName" to rule.name,
                ),
            )
        }
        (accIssues + issues) to issueDiagnostics
    } catch (e: Exception) {
        logger.error(e) { "Custom rule '${rule.ruleId}' failed" }
        val errorIssue = CompatibilityIssue(
            ruleId = rule.ruleId,
            message = "Custom rule evaluation failed: ${e.message}",
            severity = Severity.ERROR,
            affectedLibraries = emptyList(),
            suggestion = "Check custom rule handler implementation for '${rule.ruleId}'",
        )
        (accIssues + errorIssue) to (accDiag + Diagnostics.error(
            code = DiagnosticCodes.Compatibility.CUSTOM_RULE_FAILED,
            message = "Custom rule '${rule.ruleId}' failed: ${e.message}",
            processorId = id,
            context = mapOf(
                "ruleId" to rule.ruleId,
                "error" to (e.message ?: "unknown"),
            ),
        ))
    }
}
