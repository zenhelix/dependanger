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
                addProcessor(FakeProcessor("p1", ProcessingPhase.VALIDATION, order = 10))
            }
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("p1")
        }

        @Test
        fun `multiple processors with different orders build successfully`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("p1", ProcessingPhase.PROFILE, order = 5))
                addProcessor(FakeProcessor("p2", ProcessingPhase.VALIDATION, order = 65))
                addProcessor(FakeProcessor("p3", ProcessingPhase.USED_VERSIONS, order = 60))
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
                    addProcessor(FakeProcessor("dup", ProcessingPhase.PROFILE, order = 5))
                    addProcessor(FakeProcessor("dup", ProcessingPhase.VALIDATION, order = 65))
                }
            }.isInstanceOf(PipelineConfigurationException::class.java)
                .hasMessageContaining("Duplicate processor IDs")
                .hasMessageContaining("'dup'")
        }

        @Test
        fun `all processors with same ID are excluded when that ID is disabled`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("shared-id", ProcessingPhase.PROFILE, order = 5))
                addProcessor(FakeProcessor("shared-id", ProcessingPhase.VALIDATION, order = 65))
                addProcessor(FakeProcessor("survivor", ProcessingPhase.USED_VERSIONS, order = 60))
                disable("shared-id")
            }
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("survivor")
        }
    }

    @Nested
    inner class OrderCollisionValidation {

        @Test
        fun `same order same execution mode builds successfully`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("p1", ProcessingPhase.UPDATE_CHECK, order = 100))
                addProcessor(FakeProcessor("p2", ProcessingPhase.SECURITY_CHECK, order = 100))
            }
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactlyInAnyOrder("p1", "p2")
        }

        @Test
        fun `same order different execution modes throws PipelineConfigurationException`() {
            assertThatThrownBy {
                ProcessingPipeline {
                    addProcessor(FakeProcessor("seq", ProcessingPhase.VALIDATION, order = 100))
                    addProcessor(FakeProcessor("par", ProcessingPhase.UPDATE_CHECK, order = 100))
                }
            }.isInstanceOf(PipelineConfigurationException::class.java)
                .hasMessageContaining("same order but different execution modes")
        }
    }

    @Nested
    inner class OptionalProcessors {

        @Test
        fun `optional processor excluded by default`() = runTest {
            val pipeline = ProcessingPipeline {
                addProcessor(FakeProcessor("required", ProcessingPhase.PROFILE, order = 5))
                addProcessor(FakeProcessor("opt", ProcessingPhase.VALIDATION, order = 65, isOptional = true))
            }
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("required")
        }

        @Test
        fun `optional processor included when enabled`() = runTest {
            val builder = PipelineBuilder()
            builder.addProcessor(FakeProcessor("opt", ProcessingPhase.VALIDATION, order = 65, isOptional = true))
            builder.enableOptional("opt")
            val pipeline = builder.build()
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("opt")
        }

        @Test
        fun `non-optional processor always included`() = runTest {
            val builder = PipelineBuilder()
            builder.addProcessor(FakeProcessor("always", ProcessingPhase.PROFILE, order = 5, isOptional = false))
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
            builder.addProcessor(FakeProcessor("p1", ProcessingPhase.PROFILE, order = 5))
            builder.addProcessor(FakeProcessor("p2", ProcessingPhase.VALIDATION, order = 65))
            builder.disable("p1")
            val pipeline = builder.build()
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("p2")
        }

        @Test
        fun `disabled optional processor excluded`() = runTest {
            val builder = PipelineBuilder()
            builder.addProcessor(FakeProcessor("opt", ProcessingPhase.VALIDATION, order = 65, isOptional = true))
            builder.addProcessor(FakeProcessor("other", ProcessingPhase.PROFILE, order = 5))
            builder.enableOptional("opt")
            builder.disable("opt")
            val pipeline = builder.build()
            val ids = runPipelineAndGetProcessorIds(pipeline)
            assertThat(ids).containsExactly("other")
        }
    }
}
