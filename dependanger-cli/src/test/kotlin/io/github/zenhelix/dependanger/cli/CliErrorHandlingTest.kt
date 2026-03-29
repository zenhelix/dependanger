package io.github.zenhelix.dependanger.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CliErrorHandlingTest {

    @TempDir
    lateinit var tempDir: Path

    @Nested
    inner class `Missing input file` {

        @Test
        fun `add-version fails with missing file`() {
            val result = CliTestSupport.runCli(
                "add-version", "v", "1.0", "-i", tempDir.resolve("nonexistent.json").toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `remove-library fails with missing file`() {
            val result = CliTestSupport.runCli(
                "remove-library", "lib", "-i", tempDir.resolve("nonexistent.json").toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `process fails with missing file`() {
            val result = CliTestSupport.runCli(
                "process", "-i", tempDir.resolve("nonexistent.json").toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }
    }

    @Nested
    inner class `Invalid arguments` {

        @Test
        fun `invalid maven coordinates`() {
            val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.emptyMetadata())
            val result = CliTestSupport.runCli(
                "add-library", "bad", "invalid-no-colon", "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `too many coordinate parts`() {
            val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.emptyMetadata())
            val result = CliTestSupport.runCli(
                "add-library", "bad", "a:b:c:d", "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }
    }

    @Nested
    inner class `Separate output file` {

        @Test
        fun `writing to different output preserves original`() {
            val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
            val outputFile = tempDir.resolve("output.json")
            val originalContent = metadataFile.toFile().readText()

            CliTestSupport.runCli(
                "add-version", "new-ver", "1.0",
                "-i", metadataFile.toString(), "-o", outputFile.toString(),
            )

            assertThat(metadataFile.toFile().readText()).isEqualTo(originalContent)
            val output = CliTestSupport.readMetadata(outputFile)
            assertThat(output.versions.any { it.name == "new-ver" }).isTrue()
        }
    }
}
