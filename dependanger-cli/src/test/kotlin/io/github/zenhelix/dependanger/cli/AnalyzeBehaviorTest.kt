package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssuesExtensionKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class AnalyzeBehaviorTest {

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
        fun `succeeds when issues found`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    CompatibilityIssuesExtensionKey to listOf(
                        CliTestSupport.sampleCompatibilityIssue(severity = Severity.WARNING),
                        CliTestSupport.sampleCompatibilityIssue(
                            ruleId = "KOTLIN_COMPAT", severity = Severity.ERROR,
                            message = "Kotlin version mismatch",
                        ),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "analyze", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `succeeds when no issues`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(CompatibilityIssuesExtensionKey to emptyList<Nothing>())
            )

            val result = CliTestSupport.runCli(
                "analyze", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `Output file` {

        @Test
        fun `writes json report to file`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    CompatibilityIssuesExtensionKey to listOf(
                        CliTestSupport.sampleCompatibilityIssue(),
                    )
                )
            )

            val reportFile = tempDir.resolve("report.json")
            val result = CliTestSupport.runCli(
                "analyze", "-i", metadataFile.toString(), "-o", reportFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(reportFile).exists()
            val content = reportFile.readText()
            assertThat(content).contains("JDK_COMPAT")
            assertThat(content).contains("\"ruleId\"")
            assertThat(content).contains("\"severity\"")
            assertThat(content).contains("\"message\"")
            assertThat(content).contains("\"affectedLibraries\"")
        }

        @Test
        fun `filters by rule id in output file`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    CompatibilityIssuesExtensionKey to listOf(
                        CliTestSupport.sampleCompatibilityIssue(ruleId = "JDK_COMPAT"),
                        CliTestSupport.sampleCompatibilityIssue(
                            ruleId = "KOTLIN_COMPAT", severity = Severity.ERROR,
                            message = "Kotlin version mismatch",
                        ),
                    )
                )
            )

            val reportFile = tempDir.resolve("filtered.json")
            val result = CliTestSupport.runCli(
                "analyze", "--rules", "JDK_COMPAT",
                "-i", metadataFile.toString(), "-o", reportFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val content = reportFile.readText()
            assertThat(content).contains("JDK_COMPAT")
            assertThat(content).doesNotContain("KOTLIN_COMPAT")
        }
    }

    @Nested
    inner class `Fail on error` {

        @Test
        fun `exits 1 when error issues and fail-on-error set`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    CompatibilityIssuesExtensionKey to listOf(
                        CliTestSupport.sampleCompatibilityIssue(severity = Severity.ERROR),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "analyze", "--fail-on-error", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `exits 0 when only warnings and fail-on-error set`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    CompatibilityIssuesExtensionKey to listOf(
                        CliTestSupport.sampleCompatibilityIssue(severity = Severity.WARNING),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "analyze", "--fail-on-error", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `Error handling` {

        @Test
        fun `fails when input file not found`() {
            val result = CliTestSupport.runCli(
                "analyze", "-i", tempDir.resolve("nonexistent.json").toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
