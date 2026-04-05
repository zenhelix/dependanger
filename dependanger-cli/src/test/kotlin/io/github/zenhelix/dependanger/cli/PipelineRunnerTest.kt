package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.testing.test
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PipelineRunnerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `runs pipeline and invokes handler`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        CliTestSupport.mockDependangerResult().use {
            val command = object : CliktCommand(name = "test") {
                val opts by PipelineOptions()

                override fun run() {
                    PipelineRunner(this, opts).run(
                        handle = { result ->
                            formatter.success("Pipeline completed: ${result.isSuccess}")
                        }
                    )
                }
            }

            val result = command.test(listOf("-i", metadataFile.toString()))

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("Pipeline completed: true")
        }
    }

    @Test
    fun `creates formatter with json mode based on format option`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        CliTestSupport.mockDependangerResult().use {
            val command = object : CliktCommand(name = "test") {
                val opts by PipelineOptions()

                override fun run() {
                    PipelineRunner(this, opts).run(
                        handle = { _ ->
                            formatter.success("This should be suppressed")
                        }
                    )
                }
            }

            val result = command.test(listOf("-i", metadataFile.toString(), "--format", "json"))

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).doesNotContain("This should be suppressed")
        }
    }
}
