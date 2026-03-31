package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.feature.model.updates.UpdatesExtensionKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class CheckUpdatesBehaviorTest {

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
        fun `succeeds when updates found`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    UpdatesExtensionKey to listOf(
                        CliTestSupport.sampleUpdate(updateType = UpdateType.MINOR),
                        CliTestSupport.sampleUpdate(
                            alias = "coroutines",
                            group = "org.jetbrains.kotlinx",
                            artifact = "kotlinx-coroutines-core",
                            currentVersion = "1.10.1",
                            latestVersion = "2.0.0",
                            updateType = UpdateType.MAJOR,
                        ),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "check", "updates", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `succeeds when no updates`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(UpdatesExtensionKey to emptyList<Nothing>())
            )

            val result = CliTestSupport.runCli(
                "check", "updates", "-i", metadataFile.toString(),
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
                    UpdatesExtensionKey to listOf(
                        CliTestSupport.sampleUpdate(
                            group = "org.jetbrains.kotlin",
                            artifact = "kotlin-stdlib",
                            currentVersion = "2.1.20",
                            latestVersion = "2.2.0",
                            updateType = UpdateType.MINOR,
                        ),
                    )
                )
            )

            val outputFile = tempDir.resolve("updates.json")
            val result = CliTestSupport.runCli(
                "check", "updates", "-i", metadataFile.toString(), "-o", outputFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(outputFile).exists()
            val content = outputFile.readText()
            assertThat(content).contains("\"group\"")
            assertThat(content).contains("\"artifact\"")
            assertThat(content).contains("\"currentVersion\"")
            assertThat(content).contains("\"latestVersion\"")
            assertThat(content).contains("org.jetbrains.kotlin")
            assertThat(content).contains("kotlin-stdlib")
            assertThat(content).contains("2.2.0")
        }
    }

    @Nested
    inner class `Filtering` {

        @Test
        fun `filters by update type in output file`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    UpdatesExtensionKey to listOf(
                        CliTestSupport.sampleUpdate(
                            alias = "coroutines",
                            group = "org.jetbrains.kotlinx",
                            artifact = "kotlinx-coroutines-core",
                            currentVersion = "1.10.1",
                            latestVersion = "1.11.0",
                            updateType = UpdateType.MINOR,
                        ),
                        CliTestSupport.sampleUpdate(
                            alias = "stdlib",
                            group = "org.jetbrains.kotlin",
                            artifact = "kotlin-stdlib",
                            currentVersion = "2.1.20",
                            latestVersion = "3.0.0",
                            updateType = UpdateType.MAJOR,
                        ),
                    )
                )
            )

            val outputFile = tempDir.resolve("filtered.json")
            val result = CliTestSupport.runCli(
                "check", "updates", "--type", "MAJOR",
                "-i", metadataFile.toString(), "-o", outputFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(outputFile).exists()
            val content = outputFile.readText()
            assertThat(content).contains("org.jetbrains.kotlin")
            assertThat(content).contains("MAJOR")
            assertThat(content).doesNotContain("org.jetbrains.kotlinx")
            assertThat(content).doesNotContain("MINOR")
        }
    }

    @Nested
    inner class `Fail on updates` {

        @Test
        fun `exits with code 1 when updates found and fail-on-updates set`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(UpdatesExtensionKey to listOf(CliTestSupport.sampleUpdate()))
            )

            val result = CliTestSupport.runCli(
                "check", "updates", "--fail-on-updates", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `exits with code 0 when no updates and fail-on-updates set`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(UpdatesExtensionKey to emptyList<Nothing>())
            )

            val result = CliTestSupport.runCli(
                "check", "updates", "--fail-on-updates", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `Error handling` {

        @Test
        fun `fails when input file not found`() {
            val result = CliTestSupport.runCli(
                "check", "updates", "-i", tempDir.resolve("nonexistent.json").toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
