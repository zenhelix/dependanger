package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.effective.spi.ReportFormat
import io.github.zenhelix.dependanger.effective.spi.ReportSection
import io.github.zenhelix.dependanger.effective.spi.ReportSettings
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ReportGenerationBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) { preset(preset) }


    @Nested
    inner class `Basic report generation` {

        @Test
        fun `JSON report contains library data`() = runTest {
            val result = dependanger {
                versions { version("ktor", "3.1.1") }
                libraries { library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) }
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
            assertThat(report.content).contains("ktor-core")
            assertThat(report.content).contains("io.ktor")
        }

        @Test
        fun `MARKDOWN report contains section headers`() = runTest {
            val result = dependanger {
                libraries { library("assertj", "org.assertj:assertj-core:3.27.3") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val report = result.generateReport(ReportSettings.DEFAULT)

            assertThat(report.format).isEqualTo(ReportFormat.MARKDOWN)
            assertThat(report.content).contains("#")
        }

        @Test
        fun `HTML report wraps content in html and body tags`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
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
            assertThat(report.content).containsIgnoringCase("</body>")
        }

        @Test
        fun `report for empty catalog still generates valid output`() = runTest {
            val result = dependanger {}.process()

            assertThat(result.isSuccess).isTrue()
            val report = result.generateReport(ReportSettings.DEFAULT)

            assertThat(report.content).isNotBlank()
        }
    }

    @Nested
    inner class `Report with feature data` {

        @Test
        fun `report includes update info when UpdatesExtensionKey has data`() = runTest {
            val fakeUpdates = listOf(
                UpdateAvailableInfo(
                    alias = "ktor-core",
                    coordinate = MavenCoordinate("io.ktor", "ktor-client-core"),
                    currentVersion = "3.1.0",
                    latestVersion = "3.1.1",
                    latestStable = "3.1.1",
                    latestAny = "3.2.0-beta1",
                    updateType = UpdateType.PATCH,
                    repository = "https://repo1.maven.org/maven2",
                ),
            )

            val result = Dependanger({
                                         versions { version("ktor", "3.1.0") }
                                         libraries { library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeUpdateCheck { fakeUpdates })
            }.process()

            assertThat(result.isSuccess).isTrue()
            val report = result.generateReport(ReportSettings.DEFAULT)

            assertThat(report.content).contains("ktor-core")
            assertThat(report.content).contains("3.1.1")
        }

        @Test
        fun `report includes vulnerability info when VulnerabilitiesExtensionKey has data`() = runTest {
            val fakeVulns = listOf(
                VulnerabilityInfo(
                    id = "GHSA-abc-123",
                    aliases = listOf("CVE-2024-1234"),
                    summary = "Remote code execution in example-lib",
                    severity = VulnerabilitySeverity.CRITICAL,
                    cvssScore = 9.8,
                    cvssVersion = "3.1",
                    fixedVersion = "2.0.0",
                    url = "https://github.com/advisories/GHSA-abc-123",
                    affectedCoordinate = MavenCoordinate("com.example", "example-lib"),
                    affectedVersion = "1.0.0",
                ),
            )

            val result = Dependanger({
                                         libraries { library("example-lib", "com.example:example-lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeSecurityCheck { fakeVulns })
            }.process()

            assertThat(result.isSuccess).isTrue()
            val report = result.generateReport(ReportSettings.DEFAULT)

            assertThat(report.content).contains("CVE-2024-1234")
            assertThat(report.content).contains("CRITICAL")
            assertThat(report.content).contains("example-lib")
        }

        @Test
        fun `report with both updates and vulnerabilities includes both`() = runTest {
            val result = Dependanger({
                                         libraries { library("lib", "com.example:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeUpdateCheck {
                    listOf(
                        UpdateAvailableInfo(
                            alias = "lib", coordinate = MavenCoordinate("com.example", "lib"),
                            currentVersion = "1.0.0", latestVersion = "2.0.0",
                            updateType = UpdateType.MAJOR,
                        )
                    )
                })
                addProcessor(fakeSecurityCheck {
                    listOf(
                        VulnerabilityInfo(
                            id = "CVE-2024-9999", aliases = emptyList(),
                            summary = "Critical vuln", severity = VulnerabilitySeverity.CRITICAL,
                            cvssScore = 9.0, cvssVersion = "3.1", fixedVersion = "2.0.0",
                            url = null, affectedCoordinate = MavenCoordinate("com.example", "lib"), affectedVersion = "1.0.0",
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            val report = result.generateReport(ReportSettings.DEFAULT)

            assertThat(report.content).contains("2.0.0")
            assertThat(report.content).contains("CVE-2024-9999")
        }
    }

    @Nested
    inner class `Report sections filtering` {

        @Test
        fun `report with only SUMMARY section is shorter than report with all sections`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                    library("assertj", "org.assertj:assertj-core:3.27.3")
                }
                bundles { bundle("kotlin") { libraries("kotlin-stdlib") } }
            }.process()

            assertThat(result.isSuccess).isTrue()

            val fullReport = result.generateReport(
                ReportSettings(
                    format = ReportFormat.MARKDOWN,
                    outputDir = "build/reports",
                    sections = ReportSection.entries,
                )
            )

            val summaryOnlyReport = result.generateReport(
                ReportSettings(
                    format = ReportFormat.MARKDOWN,
                    outputDir = "build/reports",
                    sections = listOf(ReportSection.SUMMARY),
                )
            )

            assertThat(summaryOnlyReport.content.length).isLessThan(fullReport.content.length)
        }

        @Test
        fun `report with LIBRARIES section contains library names`() = runTest {
            val result = dependanger {
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                    library("assertj", "org.assertj:assertj-core:3.27.3")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()

            val report = result.generateReport(
                ReportSettings(
                    format = ReportFormat.MARKDOWN,
                    outputDir = "build/reports",
                    sections = listOf(ReportSection.LIBRARIES),
                )
            )

            assertThat(report.content).contains("ktor-core")
            assertThat(report.content).contains("assertj")
        }
    }

    @Nested
    inner class `Report with distribution` {

        @Test
        fun `report for filtered distribution only mentions filtered libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("android-lib", "com.android:lib:1.0") { tags("android") }
                    library("server-lib", "com.server:lib:2.0") { tags("server") }
                    library("common-lib", "com.common:lib:3.0") { tags("common") }
                }
                distributions {
                    distribution("android") {
                        spec { byTags { include { anyOf("android", "common") } } }
                    }
                }
            }.process(distribution = "android")

            assertThat(result.isSuccess).isTrue()

            val report = result.generateReport(
                ReportSettings(
                    format = ReportFormat.MARKDOWN,
                    outputDir = "build/reports",
                    sections = listOf(ReportSection.LIBRARIES),
                )
            )

            assertThat(report.content).contains("android-lib")
            assertThat(report.content).contains("common-lib")
            assertThat(report.content).doesNotContain("server-lib")
        }
    }

    @Nested
    inner class `Report with deprecations` {

        @Test
        fun `deprecated library appears in report content`() = runTest {
            val result = dependanger {
                libraries {
                    library("old-lib", "com.example:old-lib:1.0.0") {
                        deprecated(replacedBy = "new-lib", message = "Use new-lib instead")
                    }
                    library("new-lib", "com.example:new-lib:2.0.0")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()

            val report = result.generateReport(ReportSettings.DEFAULT)

            assertThat(report.content).contains("old-lib")
            assertThat(report.content).containsAnyOf("deprecated", "DEPRECATED", "Deprecated")
        }
    }
}
