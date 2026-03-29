package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.feature.model.license.LicenseCategory
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationType
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationsExtensionKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class LicenseCheckBehaviorTest {

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
    inner class `Successful checks` {

        @Test
        fun `succeeds when no violations`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(LicenseViolationsExtensionKey to emptyList<Nothing>())
            )

            val result = CliTestSupport.runCli(
                "license-check", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }

        @Test
        fun `succeeds with NOT_ALLOWED violations without fail-on-unknown`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    LicenseViolationsExtensionKey to listOf(
                        CliTestSupport.sampleLicenseViolation(
                            license = null,
                            category = LicenseCategory.UNKNOWN,
                            violationType = LicenseViolationType.NOT_ALLOWED,
                        ),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "license-check", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `Output file` {

        @Test
        fun `writes json report to file with NOT_ALLOWED violations`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    LicenseViolationsExtensionKey to listOf(
                        CliTestSupport.sampleLicenseViolation(
                            license = null,
                            category = LicenseCategory.UNKNOWN,
                            violationType = LicenseViolationType.NOT_ALLOWED,
                        ),
                    )
                )
            )

            val outputFile = tempDir.resolve("license-report.json")
            val result = CliTestSupport.runCli(
                "license-check", "-i", metadataFile.toString(), "-o", outputFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(outputFile).exists()
            val content = outputFile.readText()
            assertThat(content).contains("org.example")
            assertThat(content).contains("NOT_ALLOWED")
        }
    }

    @Nested
    inner class `Fail on denied` {

        @Test
        fun `exits 1 when denied violations exist`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    LicenseViolationsExtensionKey to listOf(
                        CliTestSupport.sampleLicenseViolation(
                            violationType = LicenseViolationType.DENIED,
                        ),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "license-check", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `exits 0 when no denied violations`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    LicenseViolationsExtensionKey to listOf(
                        CliTestSupport.sampleLicenseViolation(
                            license = null,
                            category = LicenseCategory.UNKNOWN,
                            violationType = LicenseViolationType.NOT_ALLOWED,
                        ),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "license-check", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `Fail on unknown` {

        @Test
        fun `exits 1 when unknown and fail-on-unknown set`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    LicenseViolationsExtensionKey to listOf(
                        CliTestSupport.sampleLicenseViolation(
                            license = null,
                            category = LicenseCategory.UNKNOWN,
                            violationType = LicenseViolationType.NOT_ALLOWED,
                        ),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "license-check", "-i", metadataFile.toString(), "--fail-on-unknown",
            )

            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `exits 0 when unknown but fail-on-unknown not set`() {
            mock = CliTestSupport.mockDependangerResult(
                mapOf(
                    LicenseViolationsExtensionKey to listOf(
                        CliTestSupport.sampleLicenseViolation(
                            license = null,
                            category = LicenseCategory.UNKNOWN,
                            violationType = LicenseViolationType.NOT_ALLOWED,
                        ),
                    )
                )
            )

            val result = CliTestSupport.runCli(
                "license-check", "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Nested
    inner class `Error handling` {

        @Test
        fun `fails when input file not found`() {
            val result = CliTestSupport.runCli(
                "license-check", "-i", tempDir.resolve("nonexistent.json").toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
