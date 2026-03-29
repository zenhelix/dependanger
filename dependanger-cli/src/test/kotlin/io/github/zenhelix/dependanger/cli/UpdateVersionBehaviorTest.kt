package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.VersionReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class UpdateVersionBehaviorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @Nested
    inner class `Updating named versions` {

        @Test
        fun `updates version value`() {
            val result = CliTestSupport.runCli("update-version", "kotlin", "2.2.0", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            val kotlin = metadata.versions.find { it.name == "kotlin" }
            assertThat(kotlin).isNotNull
            assertThat(kotlin!!.value).isEqualTo("2.2.0")
        }

        @Test
        fun `writes to output file when specified`() {
            val outputFile = tempDir.resolve("output.json")

            val result = CliTestSupport.runCli(
                "update-version", "kotlin", "2.2.0",
                "-i", metadataFile.toString(),
                "-o", outputFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)

            val original = CliTestSupport.readMetadata(metadataFile)
            assertThat(original.versions.find { it.name == "kotlin" }!!.value).isEqualTo("2.1.20")

            val updated = CliTestSupport.readMetadata(outputFile)
            assertThat(updated.versions.find { it.name == "kotlin" }!!.value).isEqualTo("2.2.0")
        }

        @Test
        fun `updates in place by default`() {
            val result = CliTestSupport.runCli("update-version", "coroutines", "1.11.0", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions.find { it.name == "coroutines" }!!.value).isEqualTo("1.11.0")
        }
    }

    @Nested
    inner class `Updating library versions` {

        @Test
        fun `updates library version with flag`() {
            val result = CliTestSupport.runCli(
                "update-version", "stdlib", "3.0.0", "-l",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            val lib = metadata.libraries.find { it.alias == "stdlib" }!!
            assertThat(lib.version).isEqualTo(VersionReference.Literal(version = "3.0.0"))
        }
    }

    @Nested
    inner class `Error handling` {

        @Test
        fun `fails for nonexistent version alias`() {
            val result = CliTestSupport.runCli("update-version", "nonexistent", "1.0", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `fails for nonexistent library alias`() {
            val result = CliTestSupport.runCli(
                "update-version", "nonexistent", "1.0", "-l",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `fails when input file not found`() {
            val result = CliTestSupport.runCli(
                "update-version", "kotlin", "2.2.0",
                "-i", tempDir.resolve("nonexistent.json").toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
