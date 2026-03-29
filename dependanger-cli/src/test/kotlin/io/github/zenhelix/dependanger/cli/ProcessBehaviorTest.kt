package io.github.zenhelix.dependanger.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class ProcessBehaviorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @Nested
    inner class `Processing metadata` {

        @Test
        fun `produces effective json`() {
            val effectiveFile = tempDir.resolve(CliDefaults.EFFECTIVE_OUTPUT_FILE)
            val result = CliTestSupport.runCli(
                "process", "-i", metadataFile.toString(), "-o", effectiveFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(effectiveFile).exists()
            val content = effectiveFile.readText()
            assertThat(content).contains("versions")
            assertThat(content).contains("libraries")
        }

        @Test
        fun `works with MINIMAL preset`() {
            val effectiveFile = tempDir.resolve(CliDefaults.EFFECTIVE_OUTPUT_FILE)
            val result = CliTestSupport.runCli(
                "process", "--preset", "MINIMAL",
                "-i", metadataFile.toString(), "-o", effectiveFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(effectiveFile).exists()
        }

        @Test
        fun `rejects unknown preset`() {
            val result = CliTestSupport.runCli(
                "process", "--preset", "NONEXISTENT",
                "-i", metadataFile.toString(), "-o", tempDir.resolve(CliDefaults.EFFECTIVE_OUTPUT_FILE).toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `processes empty metadata`() {
            val emptyMeta = CliTestSupport.writeMetadata(tempDir.resolve("empty"), CliTestSupport.emptyMetadata())
            val effectiveFile = tempDir.resolve(CliDefaults.EFFECTIVE_OUTPUT_FILE)
            val result = CliTestSupport.runCli(
                "process", "-i", emptyMeta.toString(), "-o", effectiveFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(effectiveFile).exists()
        }
    }
}
