package io.github.zenhelix.dependanger.integration.generators

import io.github.zenhelix.dependanger.api.generateReport
import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.api.vulnerabilities
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.spi.ReportFormat
import io.github.zenhelix.dependanger.effective.spi.ReportSection
import io.github.zenhelix.dependanger.effective.spi.ReportSettings
import io.github.zenhelix.dependanger.feature.model.settings.security.securityCheck
import io.github.zenhelix.dependanger.feature.model.settings.updates.updateCheck
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.integration.support.OsvVulnResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReportGenerationE2ETest : IntegrationTestBase() {

    @Test
    fun `generates JSON report with all sections`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            versions { version("ktor", "3.1.1") }
            libraries {
                library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val report = result.generateReport(
            ReportSettings(
                format = ReportFormat.JSON,
                outputDir = "build/reports",
                sections = ReportSection.entries,
            )
        )

        assertThat(report.format).isEqualTo(ReportFormat.JSON)
        assertThat(report.content).isNotBlank()
        assertThat(report.content).contains("ktor-core")
        assertThat(report.content).contains("io.ktor")
    }

    @Test
    fun `generates Markdown report`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            libraries {
                library("assertj", "org.assertj:assertj-core:3.27.3")
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val report = result.generateReport(ReportSettings.DEFAULT)

        assertThat(report.format).isEqualTo(ReportFormat.MARKDOWN)
        assertThat(report.content).contains("#")
        assertThat(report.content).contains("assertj")
        assertThat(report.content).contains("kotlin-stdlib")
    }

    @Test
    fun `generates HTML report`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            libraries {
                library("lib", "com.example:lib:1.0.0")
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val report = result.generateReport(
            ReportSettings(
                format = ReportFormat.HTML,
                outputDir = "build/reports",
                sections = ReportSection.entries,
            )
        )

        assertThat(report.format).isEqualTo(ReportFormat.HTML)
        assertThat(report.content).containsIgnoringCase("<html")
        assertThat(report.content).containsIgnoringCase("<body")
        assertThat(report.content).containsIgnoringCase("</html>")
    }

    @Test
    fun `report includes feature data when available`() = runTest {
        mockHttp {
            maven {
                metadata("com.example", "lib", listOf("1.0.0", "2.0.0"))
            }
            osv {
                vulnerabilities(
                    "com.example:lib", listOf(
                        OsvVulnResponse(
                            id = "GHSA-report-test",
                            summary = "Test vulnerability for report",
                            cvssScore = 8.5,
                            fixedVersion = "2.0.0"
                        )
                    )
                )
            }
        }

        val result = dependanger(ProcessingPreset.STRICT) {
            libraries {
                library("lib", "com.example:lib:1.0.0")
            }
            settings {
                updateCheck {
                    enabled = true
                    cacheDirectory = cacheDirFor("versions")
                    repositories = listOf(
                        MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                    )
                }
                securityCheck {
                    enabled = true
                    cacheDirectory = cacheDirFor("security")
                    failOnVulnerability = io.github.zenhelix.dependanger.core.model.Severity.INFO
                }
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.updates).isNotEmpty()
        assertThat(result.vulnerabilities).isNotEmpty()

        val report = result.generateReport(
            ReportSettings(
                format = ReportFormat.MARKDOWN,
                outputDir = "build/reports",
                sections = ReportSection.entries,
            )
        )

        assertThat(report.content).contains("2.0.0")
        assertThat(report.content).contains("GHSA-report-test")
    }
}
