package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class InitBehaviorTest {

    @TempDir
    lateinit var tempDir: Path

    @Nested
    inner class `Creating new metadata` {

        @Test
        fun `creates metadata json with empty structure`() {
            val output = tempDir.resolve("metadata.json")

            val result = CliTestSupport.runCli("init", "-o", output.toString())

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(output).exists()

            val metadata = CliTestSupport.readMetadata(output)
            assertThat(metadata.versions).isEmpty()
            assertThat(metadata.libraries).isEmpty()
            assertThat(metadata.plugins).isEmpty()
            assertThat(metadata.bundles).isEmpty()
            assertThat(metadata.schemaVersion).isEqualTo(DependangerMetadata.SCHEMA_VERSION)
        }

        @Test
        fun `creates file at custom path`() {
            val output = tempDir.resolve("custom/path/registry.json")

            val result = CliTestSupport.runCli("init", "-o", output.toString())

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(output).exists()
        }
    }

    @Nested
    inner class `Overwrite protection` {

        @Test
        fun `fails when file already exists and preserves content`() {
            val output = tempDir.resolve("metadata.json")
            val originalContent = "{}"
            output.writeText(originalContent)

            val result = CliTestSupport.runCli("init", "-o", output.toString())

            assertThat(result.statusCode).isEqualTo(1)
            assertThat(output.readText()).isEqualTo(originalContent)
        }

        @Test
        fun `force flag overwrites existing file`() {
            val output = tempDir.resolve("metadata.json")
            output.writeText("{}")

            val result = CliTestSupport.runCli("init", "--force", "-o", output.toString())

            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(output)
            assertThat(metadata.versions).isEmpty()
        }
    }
}
