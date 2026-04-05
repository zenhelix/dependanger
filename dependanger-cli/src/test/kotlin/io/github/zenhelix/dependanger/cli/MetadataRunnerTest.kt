package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.testing.test
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Version
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MetadataRunnerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `runs read-transform-write cycle`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        val command = object : CliktCommand(name = "test") {
            val opts by MetadataOptions()

            override fun run() {
                MetadataRunner(this, opts).run { metadata ->
                    val newVersion = Version(name = "test-version", value = "1.0.0", fallbacks = emptyList())
                    metadata.copy(versions = metadata.versions + newVersion) to "Added test-version"
                }
            }
        }

        val result = command.test(listOf("-i", metadataFile.toString()))

        assertThat(result.statusCode).isEqualTo(0)
        assertThat(result.output).contains("[OK]")
        val updated = CliTestSupport.readMetadata(metadataFile)
        assertThat(updated.versions.any { it.name == "test-version" }).isTrue()
    }

    @Test
    fun `writes to separate output file when specified`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
        val outputFile = tempDir.resolve("output.json")

        val command = object : CliktCommand(name = "test") {
            val opts by MetadataOptions()

            override fun run() {
                MetadataRunner(this, opts).run { metadata ->
                    val newVersion = Version(name = "out-version", value = "2.0.0", fallbacks = emptyList())
                    metadata.copy(versions = metadata.versions + newVersion) to "Added out-version"
                }
            }
        }

        val result = command.test(listOf("-i", metadataFile.toString(), "-o", outputFile.toString()))

        assertThat(result.statusCode).isEqualTo(0)
        val updated = CliTestSupport.readMetadata(outputFile)
        assertThat(updated.versions.any { it.name == "out-version" }).isTrue()
        val original = CliTestSupport.readMetadata(metadataFile)
        assertThat(original.versions.none { it.name == "out-version" }).isTrue()
    }

    @Test
    fun `handles CliException with error output`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        val command = object : CliktCommand(name = "test") {
            val opts by MetadataOptions()

            override fun run() {
                MetadataRunner(this, opts).run { _ ->
                    throw CliException.AliasNotFound("Test", "missing")
                }
            }
        }

        val result = command.test(listOf("-i", metadataFile.toString()))

        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `readAndHandle provides metadata and write function`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        val command = object : CliktCommand(name = "test") {
            val opts by MetadataOptions()

            override fun run() {
                MetadataRunner(this, opts).readAndHandle {
                    val newVersion = Version(name = "ctx-version", value = "3.0.0", fallbacks = emptyList())
                    val updated = metadata.copy(versions = metadata.versions + newVersion)
                    write(updated)
                    formatter.success("Done via context")
                }
            }
        }

        val result = command.test(listOf("-i", metadataFile.toString()))

        assertThat(result.statusCode).isEqualTo(0)
        assertThat(result.output).contains("Done via context")
        val updated = CliTestSupport.readMetadata(metadataFile)
        assertThat(updated.versions.any { it.name == "ctx-version" }).isTrue()
    }
}
