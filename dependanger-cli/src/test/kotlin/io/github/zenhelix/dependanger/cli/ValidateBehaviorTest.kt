package io.github.zenhelix.dependanger.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ValidateBehaviorTest {

    @TempDir
    lateinit var tempDir: Path

    @Nested
    inner class `Validating metadata` {

        @Test
        fun `valid metadata passes validation`() {
            val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
            val result = CliTestSupport.runCli("validate", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `empty metadata passes validation`() {
            val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.emptyMetadata())
            val result = CliTestSupport.runCli("validate", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `json format completes successfully`() {
            val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
            val result = CliTestSupport.runCli("validate", "--format", "json", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `Missing input` {

        @Test
        fun `fails when metadata file does not exist`() {
            val result = CliTestSupport.runCli("validate", "-i", tempDir.resolve("nonexistent.json").toString())
            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
