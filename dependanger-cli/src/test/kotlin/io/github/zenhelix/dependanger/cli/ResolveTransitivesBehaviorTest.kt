package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.features.transitive.model.TransitivesExtensionKey
import io.github.zenhelix.dependanger.features.transitive.model.VersionConflictsExtensionKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class ResolveTransitivesBehaviorTest {

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
    inner class `Text output` {

        @Test
        fun `succeeds with dependency tree`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    TransitivesExtensionKey to listOf(CliTestSupport.sampleTransitiveTree()),
                    VersionConflictsExtensionKey to emptyList<Nothing>(),
                )
            )

            val result = CliTestSupport.runCli(
                "resolve-transitives", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `succeeds with conflicts`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    TransitivesExtensionKey to listOf(CliTestSupport.sampleTransitiveTree()),
                    VersionConflictsExtensionKey to listOf(CliTestSupport.sampleVersionConflict()),
                )
            )

            val result = CliTestSupport.runCli(
                "resolve-transitives", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `succeeds when no transitives`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    TransitivesExtensionKey to emptyList<Nothing>(),
                    VersionConflictsExtensionKey to emptyList<Nothing>(),
                )
            )

            val result = CliTestSupport.runCli(
                "resolve-transitives", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `JSON output` {

        @Test
        fun `writes json to output file`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    TransitivesExtensionKey to listOf(CliTestSupport.sampleTransitiveTree()),
                    VersionConflictsExtensionKey to listOf(CliTestSupport.sampleVersionConflict()),
                )
            )

            val outputFile = tempDir.resolve("transitives.json")
            val result = CliTestSupport.runCli(
                "resolve-transitives", "-i", metadataFile.toString(), "-o", outputFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(outputFile).exists()
            val content = outputFile.readText()
            assertThat(content).contains("transitives")
            assertThat(content).contains("versionConflicts")
            assertThat(content).contains("org.jetbrains.kotlin")
            assertThat(content).contains("kotlin-stdlib")
        }
    }

    @Nested
    inner class `Conflict resolution strategy` {

        @Test
        fun `rejects invalid strategy`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    TransitivesExtensionKey to emptyList<Nothing>(),
                    VersionConflictsExtensionKey to emptyList<Nothing>(),
                )
            )

            val result = CliTestSupport.runCli(
                "resolve-transitives", "--conflict-resolution", "INVALID", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }

    @Nested
    inner class `Error handling` {

        @Test
        fun `fails when input file not found`() {
            val result = CliTestSupport.runCli(
                "resolve-transitives", "-i", tempDir.resolve("nonexistent.json").toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
