package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PipelineBuilderTest {

    private fun emptyRawMetadata(): DependangerMetadata = DependangerMetadata(
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

    private fun context(): ProcessingContext {
        val metadata = emptyRawMetadata()
        return ProcessingContext(
            originalMetadata = metadata,
            settings = metadata.settings,
            environment = ProcessingEnvironment.DEFAULT,
            activeDistribution = null,
            callback = null,
            properties = emptyMap(),
        )
    }

    private fun runPipelineAndGetProcessorIds(pipeline: ProcessingPipeline): List<String> = kotlinx.coroutines.runBlocking {
        pipeline.process(context()).processingInfo!!.processorIds
    }

    @Nested
    inner class Building {

        @Test
        fun `empty pipeline builds successfully`() = runTest {
            val pipeline = ProcessingPipeline {}
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).isEmpty()
        }

        @Test
        fun `single processor builds and executes`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("p1", ProcessingPhase.VALIDATION))
            }
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("p1")
        }

        @Test
        fun `multiple processors with different orders build successfully`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("p1", ProcessingPhase.PROFILE))
                addProcessor(FakeProcessor("p2", ProcessingPhase.VALIDATION, constraints = setOf(OrderConstraint.runsAfter("p3"))))
                addProcessor(FakeProcessor("p3", ProcessingPhase.USED_VERSIONS, constraints = setOf(OrderConstraint.runsAfter("p1"))))
            }
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("p1", "p3", "p2")
        }
    }

    @Nested
    inner class DuplicateIdValidation {

        @Test
        fun `duplicate processor IDs throws PipelineConfigurationException`() {
            assertThatThrownBy {
                ProcessingPipeline {
                    addProcessor(FakeProcessor("dup", ProcessingPhase.PROFILE))
                    addProcessor(FakeProcessor("dup", ProcessingPhase.VALIDATION))
                }
            }.isInstanceOf(PipelineConfigurationException::class.java)
                .hasMessageContaining("Duplicate processor IDs")
                .hasMessageContaining("'dup'")
        }

        @Test
        fun `all processors with same ID are excluded when that ID is disabled`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("shared-id", ProcessingPhase.PROFILE))
                addProcessor(FakeProcessor("shared-id", ProcessingPhase.VALIDATION))
                addProcessor(FakeProcessor("survivor", ProcessingPhase.USED_VERSIONS))
                disable("shared-id")
            }
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("survivor")
        }
    }

    @Nested
    inner class TopologicalSortValidation {

        @Test
        fun `processors without constraints are sorted alphabetically`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("p2", ProcessingPhase.VALIDATION))
                addProcessor(FakeProcessor("p1", ProcessingPhase.VALIDATION))
            }
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("p1", "p2")
        }

        @Test
        fun `circular dependency throws PipelineConfigurationException`() {
            assertThatThrownBy {
                ProcessingPipeline {
                    addProcessor(FakeProcessor("a", ProcessingPhase.VALIDATION, constraints = setOf(OrderConstraint.runsAfter("b"))))
                    addProcessor(FakeProcessor("b", ProcessingPhase.VALIDATION, constraints = setOf(OrderConstraint.runsAfter("a"))))
                }
            }.isInstanceOf(PipelineConfigurationException::class.java)
                .hasMessageContaining("Circular dependency")
        }
    }

    @Nested
    inner class OptionalProcessors {

        @Test
        fun `optional processor excluded by default`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("required", ProcessingPhase.PROFILE))
                addProcessor(FakeProcessor("opt", ProcessingPhase.VALIDATION, isOptional = true))
            }
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("required")
        }

        @Test
        fun `optional processor included when enabled`() = runTest {
            val builder = PipelineBuilder()
            builder.addProcessor(FakeProcessor("opt", ProcessingPhase.VALIDATION, isOptional = true))
            builder.enableOptional("opt")
            val pipeline = builder.build()
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("opt")
        }

        @Test
        fun `non-optional processor always included`() = runTest {
            val builder = PipelineBuilder()
            builder.addProcessor(FakeProcessor("always", ProcessingPhase.PROFILE, isOptional = false))
            val pipeline = builder.build()
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("always")
        }
    }

    @Nested
    inner class DisabledProcessors {

        @Test
        fun `disabled processor excluded from pipeline`() = runTest {
            val builder = PipelineBuilder()
            builder.addProcessor(FakeProcessor("p1", ProcessingPhase.PROFILE))
            builder.addProcessor(FakeProcessor("p2", ProcessingPhase.VALIDATION))
            builder.disable("p1")
            val pipeline = builder.build()
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("p2")
        }

        @Test
        fun `disabled optional processor excluded`() = runTest {
            val builder = PipelineBuilder()
            builder.addProcessor(FakeProcessor("opt", ProcessingPhase.VALIDATION, isOptional = true))
            builder.addProcessor(FakeProcessor("other", ProcessingPhase.PROFILE))
            builder.enableOptional("opt")
            builder.disable("opt")
            val pipeline = builder.build()
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("other")
        }
    }
}
