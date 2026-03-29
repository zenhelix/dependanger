package io.github.zenhelix.dependanger.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ReportBehaviorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path
    private var mock: AutoCloseable? = null

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @AfterEach
    fun tearDown() {
        mock?.close()
    }

    @Nested
    inner class `Report generation` {

        @Test
        fun `generates markdown report`() {
            mock = CliTestSupport.mockDependangerResult()

            val outputDir = tempDir.resolve("reports")
            outputDir.toFile().mkdirs()

            val result = CliTestSupport.runCli(
                "report", "-i", metadataFile.toString(), "-o", outputDir.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `generates json report`() {
            mock = CliTestSupport.mockDependangerResult()

            val outputDir = tempDir.resolve("reports")
            outputDir.toFile().mkdirs()

            val result = CliTestSupport.runCli(
                "report", "--format", "json",
                "-i", metadataFile.toString(), "-o", outputDir.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `Invalid arguments` {

        @Test
        fun `rejects unknown format`() {
            mock = CliTestSupport.mockDependangerResult()

            val result = CliTestSupport.runCli(
                "report", "--format", "invalid",
                "-i", metadataFile.toString(), "-o", tempDir.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `rejects unknown section`() {
            mock = CliTestSupport.mockDependangerResult()

            val result = CliTestSupport.runCli(
                "report", "--sections", "INVALID",
                "-i", metadataFile.toString(), "-o", tempDir.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }

    @Nested
    inner class `Error handling` {

        @Test
        fun `fails when input file not found`() {
            val result = CliTestSupport.runCli(
                "report", "-i", tempDir.resolve("nonexistent.json").toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
