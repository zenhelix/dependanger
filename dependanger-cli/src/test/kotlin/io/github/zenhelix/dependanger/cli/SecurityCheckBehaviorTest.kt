package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class SecurityCheckBehaviorTest {

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
        fun `succeeds when vulnerabilities found`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    VulnerabilitiesExtensionKey to listOf(
                        CliTestSupport.sampleVulnerability(severity = VulnerabilitySeverity.HIGH),
                        CliTestSupport.sampleVulnerability(
                            id = "GHSA-crit-002", severity = VulnerabilitySeverity.CRITICAL,
                        ),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "check", "security", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `succeeds when no vulnerabilities`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(VulnerabilitiesExtensionKey to emptyList<Nothing>())
            )

            val result = CliTestSupport.runCli(
                "check", "security", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `SARIF output` {

        @Test
        fun `renders sarif format via echo`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    VulnerabilitiesExtensionKey to listOf(
                        CliTestSupport.sampleVulnerability(
                            id = "GHSA-sarif-001", severity = VulnerabilitySeverity.CRITICAL,
                            summary = "SARIF test vulnerability", cvssScore = 9.0,
                        ),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "check", "security", "--sarif", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("\$schema")
            assertThat(result.output).contains("2.1.0")
            assertThat(result.output).contains("runs")
            assertThat(result.output).contains("results")
            assertThat(result.output).contains("dependanger")
            assertThat(result.output).contains("GHSA-sarif-001")
        }
    }

    @Nested
    inner class `Output file` {

        @Test
        fun `writes json report to file`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    VulnerabilitiesExtensionKey to listOf(
                        CliTestSupport.sampleVulnerability(id = "GHSA-file-001"),
                    )
                )
            )

            val outputFile = tempDir.resolve("report.json")
            val result = CliTestSupport.runCli(
                "check", "security", "-i", metadataFile.toString(), "-o", outputFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(outputFile).exists()
            assertThat(outputFile.readText()).contains("GHSA-file-001")
        }

        @Test
        fun `writes sarif report to file`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    VulnerabilitiesExtensionKey to listOf(
                        CliTestSupport.sampleVulnerability(id = "GHSA-sarif-file"),
                    )
                )
            )

            val outputFile = tempDir.resolve("report.sarif.json")
            val result = CliTestSupport.runCli(
                "check", "security", "--sarif",
                "-i", metadataFile.toString(), "-o", outputFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(outputFile).exists()
            val content = outputFile.readText()
            assertThat(content).contains("\$schema")
            assertThat(content).contains("2.1.0")
            assertThat(content).contains("GHSA-sarif-file")
        }
    }

    @Nested
    inner class `Fail on severity` {

        @Test
        fun `exits 1 when high vuln meets threshold`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    VulnerabilitiesExtensionKey to listOf(
                        CliTestSupport.sampleVulnerability(severity = VulnerabilitySeverity.HIGH),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "check", "security", "--fail-on", "HIGH", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `exits 0 when low vuln below threshold`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    VulnerabilitiesExtensionKey to listOf(
                        CliTestSupport.sampleVulnerability(severity = VulnerabilitySeverity.LOW),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "check", "security", "--fail-on", "HIGH", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `rejects invalid severity`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(VulnerabilitiesExtensionKey to emptyList<Nothing>())
            )

            val result = CliTestSupport.runCli(
                "check", "security", "--fail-on", "INVALID", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }

    @Nested
    inner class `Error handling` {

        @Test
        fun `fails when input file not found`() {
            val result = CliTestSupport.runCli(
                "check", "security", "-i", tempDir.resolve("nonexistent.json").toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
