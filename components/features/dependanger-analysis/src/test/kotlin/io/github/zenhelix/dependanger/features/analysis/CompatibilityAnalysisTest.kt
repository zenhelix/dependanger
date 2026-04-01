package io.github.zenhelix.dependanger.features.analysis

import io.github.zenhelix.dependanger.core.model.CompatibilityRule
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.compatibilityIssues
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import io.github.zenhelix.dependanger.effective.spi.CustomRuleHandler
import io.github.zenhelix.dependanger.effective.spi.CustomRuleHandlersKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CompatibilityAnalysisTest {

    private val processor = CompatibilityCheckProcessor()

    private fun noopHandler(ruleType: String): CustomRuleHandler = object : CustomRuleHandler {
        override val ruleType: String = ruleType
        override fun evaluate(
            rule: CompatibilityRule.CustomRule,
            libraries: Map<String, EffectiveLibrary>,
            context: ProcessingContext,
        ): List<CompatibilityIssue> = emptyList()
    }

    private fun buildMetadata(
        compatibility: List<CompatibilityRule> = emptyList(),
    ): DependangerMetadata = DependangerMetadata(
        schemaVersion = DependangerMetadata.SCHEMA_VERSION,
        versions = emptyList(),
        libraries = emptyList(),
        plugins = emptyList(),
        bundles = emptyList(),
        bomImports = emptyList(),
        constraints = emptyList(),
        targetPlatforms = emptyList(),
        distributions = emptyList(),
        compatibility = compatibility,
        settings = Settings.DEFAULT,
        presets = emptyList(),
        extensions = emptyMap(),
    )

    private fun buildEffectiveMetadata(
        libraries: Map<String, EffectiveLibrary> = emptyMap(),
        diagnostics: Diagnostics = Diagnostics.EMPTY,
    ): EffectiveMetadata = EffectiveMetadata(
        schemaVersion = DependangerMetadata.SCHEMA_VERSION,
        distribution = null,
        versions = emptyMap(),
        libraries = libraries,
        plugins = emptyMap(),
        bundles = emptyMap(),
        diagnostics = diagnostics,
        processingInfo = null,
    )

    private fun buildContext(
        original: DependangerMetadata,
        handlers: Map<String, CustomRuleHandler> = emptyMap(),
    ): ProcessingContext = ProcessingContext(
        originalMetadata = original,
        settings = Settings.DEFAULT,
        environment = ProcessingEnvironment.DEFAULT,
        activeDistribution = null,
        callback = null,
        properties = if (handlers.isNotEmpty()) mapOf(CustomRuleHandlersKey to handlers) else emptyMap(),
    )

    private fun customRule(
        name: String = "test-rule",
        ruleId: String = "test-handler",
        parameters: Map<String, String> = emptyMap(),
        severity: Severity = Severity.WARNING,
        message: String? = null,
    ): CompatibilityRule.CustomRule = CompatibilityRule.CustomRule(
        name = name,
        ruleId = ruleId,
        parameters = parameters,
        severity = severity,
        message = message,
    )

    @Nested
    inner class `when no custom rules exist` {

        @Test
        fun `reports info diagnostic and returns metadata unchanged`() = runTest {
            val original = buildMetadata(compatibility = emptyList())
            val metadata = buildEffectiveMetadata()
            val context = buildContext(original)

            val result = processor.process(metadata, context)

            assertThat(result.diagnostics.infos).hasSize(1)
            assertThat(result.diagnostics.infos.first().code)
                .isEqualTo(DiagnosticCodes.Compatibility.NO_CUSTOM_RULES)
            assertThat(result.libraries).isEqualTo(metadata.libraries)
            assertThat(result.versions).isEqualTo(metadata.versions)
        }

        @Test
        fun `non-custom rules are ignored by processor`() = runTest {
            val original = buildMetadata(
                compatibility = listOf(
                    CompatibilityRule.MutualExclusion(
                        name = "exclusion",
                        libraries = listOf("a", "b"),
                        severity = Severity.ERROR,
                        message = null,
                    ),
                ),
            )
            val metadata = buildEffectiveMetadata()
            val context = buildContext(original)

            val result = processor.process(metadata, context)

            assertThat(result.diagnostics.infos).hasSize(1)
            assertThat(result.diagnostics.infos.first().code)
                .isEqualTo(DiagnosticCodes.Compatibility.NO_CUSTOM_RULES)
        }
    }

    @Nested
    inner class `when custom rule handler is found` {

        @Test
        fun `evaluates rule and returns issues from handler`() = runTest {
            val rule = customRule(ruleId = "found-handler", name = "Found Rule")
            val original = buildMetadata(compatibility = listOf(rule))
            val library = mockk<EffectiveLibrary> {
                every { alias } returns "lib-a"
                every { group } returns "com.example"
                every { artifact } returns "lib-a"
            }
            val metadata = buildEffectiveMetadata(libraries = mapOf("lib-a" to library))

            val expectedIssue = CompatibilityIssue(
                ruleId = "Found Rule",
                message = "Version conflict detected",
                severity = Severity.WARNING,
                affectedLibraries = listOf("lib-a"),
                suggestion = "Upgrade lib-a",
            )

            val handler = object : CustomRuleHandler {
                override val ruleType: String = "found-handler"
                override fun evaluate(
                    rule: CompatibilityRule.CustomRule,
                    libraries: Map<String, EffectiveLibrary>,
                    context: ProcessingContext,
                ): List<CompatibilityIssue> = listOf(expectedIssue)
            }

            val context = buildContext(original, handlers = mapOf("found-handler" to handler))
            val result = processor.process(metadata, context)

            assertThat(result.compatibilityIssues).hasSize(1)
            assertThat(result.compatibilityIssues.first().message).isEqualTo("Version conflict detected")
        }
    }

    @Nested
    inner class `when no handlers registered` {

        @Test
        fun `returns info diagnostic and skips custom rules`() = runTest {
            val rule = customRule(ruleId = "some-handler", name = "Some Rule")
            val original = buildMetadata(compatibility = listOf(rule))
            val metadata = buildEffectiveMetadata()
            val context = buildContext(original)

            val result = processor.process(metadata, context)

            assertThat(result.diagnostics.infos).anyMatch { diag ->
                diag.code == DiagnosticCodes.Compatibility.NO_CUSTOM_HANDLERS
            }
            assertThat(result.diagnostics.warnings).isEmpty()
            assertThat(result.compatibilityIssues).isEmpty()
        }
    }

    @Nested
    inner class `when handler is not found for rule type` {

        @Test
        fun `generates warning diagnostic with rule id`() = runTest {
            val rule = customRule(ruleId = "nonexistent-handler", name = "Missing Handler Rule")
            val original = buildMetadata(compatibility = listOf(rule))
            val metadata = buildEffectiveMetadata()
            val context = buildContext(original, handlers = mapOf("other-handler" to noopHandler("other-handler")))

            val result = processor.process(metadata, context)

            assertThat(result.diagnostics.warnings).hasSize(1)
            val warning = result.diagnostics.warnings.first()
            assertThat(warning.code).isEqualTo(DiagnosticCodes.Compatibility.CUSTOM_HANDLER_NOT_FOUND)
            assertThat(warning.message).contains("nonexistent-handler")
            assertThat(warning.context).containsEntry("ruleId", "nonexistent-handler")
        }

        @Test
        fun `does not produce error issues when handler is missing`() = runTest {
            val rule = customRule(ruleId = "missing-handler")
            val original = buildMetadata(compatibility = listOf(rule))
            val metadata = buildEffectiveMetadata()
            val context = buildContext(original, handlers = mapOf("other-handler" to noopHandler("other-handler")))

            val result = processor.process(metadata, context)

            assertThat(result.compatibilityIssues).isEmpty()
            assertThat(result.diagnostics.errors).isEmpty()
        }
    }

    @Nested
    inner class `multiple custom rules` {

        @Test
        fun `are evaluated in sequence and each produces a warning when handler is missing`() = runTest {
            val rule1 = customRule(ruleId = "handler-a", name = "Rule A")
            val rule2 = customRule(ruleId = "handler-b", name = "Rule B")
            val rule3 = customRule(ruleId = "handler-c", name = "Rule C")
            val original = buildMetadata(compatibility = listOf(rule1, rule2, rule3))
            val metadata = buildEffectiveMetadata()
            val context = buildContext(original, handlers = mapOf("dummy" to noopHandler("dummy")))

            val result = processor.process(metadata, context)

            assertThat(result.diagnostics.warnings).hasSize(3)
            val ruleIds = result.diagnostics.warnings.map { it.context["ruleId"] }
            assertThat(ruleIds).containsExactly("handler-a", "handler-b", "handler-c")
        }

        @Test
        fun `diagnostics accumulate across all rules`() = runTest {
            val existingDiag = Diagnostics.info(
                code = "EXISTING",
                message = "Pre-existing diagnostic",
                processorId = "other-processor",
                context = emptyMap(),
            )
            val rule1 = customRule(ruleId = "x")
            val rule2 = customRule(ruleId = "y")
            val original = buildMetadata(compatibility = listOf(rule1, rule2))
            val metadata = buildEffectiveMetadata(diagnostics = existingDiag)
            val context = buildContext(original, handlers = mapOf("dummy" to noopHandler("dummy")))

            val result = processor.process(metadata, context)

            assertThat(result.diagnostics.infos).hasSize(1)
            assertThat(result.diagnostics.infos.first().code).isEqualTo("EXISTING")
            assertThat(result.diagnostics.warnings).hasSize(2)
        }
    }

    @Nested
    inner class `processor metadata` {

        @Test
        fun `supports all contexts`() {
            val original = buildMetadata()
            val context = buildContext(original)

            assertThat(processor.supports(context)).isTrue()
        }

        @Test
        fun `is optional`() {
            assertThat(processor.isOptional).isTrue()
        }
    }
}
