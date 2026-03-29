package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.model.withExtension
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private fun emptyMetadata(): DependangerMetadata = DependangerMetadata(
    schemaVersion = "1.0",
    versions = emptyList(),
    libraries = emptyList(),
    plugins = emptyList(),
    bundles = emptyList(),
    bomImports = emptyList(),
    constraints = emptyList(),
    targetPlatforms = emptyList(),
    distributions = emptyList(),
    compatibility = emptyList(),
    settings = Settings.DEFAULT,
    presets = emptyList(),
    extensions = emptyMap(),
)

private fun context(metadata: DependangerMetadata = emptyMetadata()): ProcessingContext = ProcessingContext(
    originalMetadata = metadata,
    settings = metadata.settings,
    environment = ProcessingEnvironment.DEFAULT,
    activeDistribution = null,
    callback = null,
    properties = emptyMap(),
)

private class DiagnosticProcessor(
    override val id: String,
    override val phase: ProcessingPhase,
    override val constraints: Set<OrderConstraint> = emptySet(),
    private val diagnosticMessage: String,
    override val isOptional: Boolean = false,
    override val description: String = "test",
) : EffectiveMetadataProcessor {
    override fun supports(context: ProcessingContext): Boolean = true
    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata {
        val diag = Diagnostics.info("TEST", diagnosticMessage, id, emptyMap())
        return metadata.copy(diagnostics = metadata.diagnostics + diag)
    }
}

private class ExtensionAddingProcessor(
    override val id: String,
    override val phase: ProcessingPhase,
    override val constraints: Set<OrderConstraint> = emptySet(),
    private val extensionKey: ExtensionKey<String>,
    private val extensionValue: String,
    override val isOptional: Boolean = false,
    override val description: String = "test",
) : EffectiveMetadataProcessor {
    override fun supports(context: ProcessingContext): Boolean = true
    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata =
        metadata.withExtension(extensionKey, extensionValue)
}

private class VersionModifyingProcessor(
    override val id: String,
    override val phase: ProcessingPhase,
    override val constraints: Set<OrderConstraint> = emptySet(),
    override val isOptional: Boolean = false,
    override val description: String = "test",
) : EffectiveMetadataProcessor {
    override fun supports(context: ProcessingContext): Boolean = true
    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata =
        metadata.copy(
            versions = metadata.versions + ("fake" to ResolvedVersion("fake", "1.0", VersionSource.DECLARED, null))
        )
}

class ProcessingPipelineTest {

    @Nested
    inner class EmptyPipeline {

        @Test
        fun `empty pipeline returns initial metadata with processingInfo`() = runTest {
            val pipeline = ProcessingPipeline {}
            val result = pipeline.process(context())

            assertThat(result.processingInfo).isNotNull
            assertThat(result.processingInfo!!.processorIds).isEmpty()
            assertThat(result.versions).isEmpty()
            assertThat(result.libraries).isEmpty()
            assertThat(result.plugins).isEmpty()
            assertThat(result.bundles).isEmpty()
            assertThat(result.diagnostics.isValid).isTrue()
        }
    }

    @Nested
    inner class SequentialExecution {

        @Test
        fun `processors run in order and add diagnostics sequentially`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(DiagnosticProcessor("first", ProcessingPhase.PROFILE, diagnosticMessage = "msg-first"))
                addProcessor(DiagnosticProcessor("second", ProcessingPhase.VALIDATION, constraints = setOf(OrderConstraint.runsAfter("first")), diagnosticMessage = "msg-second"))
            }

            val result = pipeline.process(context())

            assertThat(result.diagnostics.infos).hasSize(2)
            assertThat(result.diagnostics.infos[0].message).isEqualTo("msg-first")
            assertThat(result.diagnostics.infos[1].message).isEqualTo("msg-second")
        }

        @Test
        fun `processor that does not support context is skipped`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(DiagnosticProcessor("first", ProcessingPhase.PROFILE, diagnosticMessage = "msg-first"))
                addProcessor(FakeProcessor("skipped", ProcessingPhase.METADATA_CONVERSION, constraints = setOf(OrderConstraint.runsAfter("first")), supported = false))
                addProcessor(DiagnosticProcessor("third", ProcessingPhase.VALIDATION, constraints = setOf(OrderConstraint.runsAfter("skipped")), diagnosticMessage = "msg-third"))
            }

            val result = pipeline.process(context())

            assertThat(result.diagnostics.infos).hasSize(2)
            assertThat(result.diagnostics.infos.map { it.message }).containsExactly("msg-first", "msg-third")
        }

        @Test
        fun `processingInfo contains executed processor IDs excluding skipped`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("p1", ProcessingPhase.PROFILE))
                addProcessor(FakeProcessor("skipped", ProcessingPhase.METADATA_CONVERSION, constraints = setOf(OrderConstraint.runsAfter("p1")), supported = false))
                addProcessor(FakeProcessor("p2", ProcessingPhase.VALIDATION, constraints = setOf(OrderConstraint.runsAfter("skipped"))))
            }

            val result = pipeline.process(context())

            assertThat(result.processingInfo).isNotNull
            assertThat(result.processingInfo!!.processorIds)
                .containsExactly("p1", "p2")
                .doesNotContain("skipped")
        }
    }

    @Nested
    inner class ParallelExecution {

        @Test
        fun `parallel processors can add diagnostics`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(
                    DiagnosticProcessor("update-check", ProcessingPhase.UPDATE_CHECK, diagnosticMessage = "update-diag")
                )
                addProcessor(
                    DiagnosticProcessor("security-check", ProcessingPhase.SECURITY_CHECK, diagnosticMessage = "security-diag")
                )
            }

            val result = pipeline.process(context())

            assertThat(result.diagnostics.infos).hasSize(2)
            val messages = result.diagnostics.infos.map { it.message }
            assertThat(messages).containsExactlyInAnyOrder("update-diag", "security-diag")
        }

        @Test
        fun `parallel processors can add extensions`() = runTest {
            val key1 = ExtensionKey("ext1", String.serializer())
            val key2 = ExtensionKey("ext2", String.serializer())

            val pipeline = ProcessingPipeline {
                addProcessor(
                    ExtensionAddingProcessor("ext-proc-1", ProcessingPhase.UPDATE_CHECK, extensionKey = key1, extensionValue = "value1")
                )
                addProcessor(
                    ExtensionAddingProcessor("ext-proc-2", ProcessingPhase.SECURITY_CHECK, extensionKey = key2, extensionValue = "value2")
                )
            }

            val result = pipeline.process(context())

            assertThat(result.extensions).hasSize(2)
        }

        @Test
        fun `parallel processor modifying versions throws IllegalStateException`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(
                    VersionModifyingProcessor("mod-1", ProcessingPhase.UPDATE_CHECK)
                )
                addProcessor(
                    VersionModifyingProcessor("mod-2", ProcessingPhase.SECURITY_CHECK)
                )
            }

            val thrown = assertThrows<IllegalStateException> {
                pipeline.process(context())
            }
            assertThat(thrown.message).contains("versions")
        }
    }
}
