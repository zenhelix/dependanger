package io.github.zenhelix.dependanger.features.report

import io.github.zenhelix.dependanger.core.model.DeprecationInfo
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.ReportFormat
import io.github.zenhelix.dependanger.core.model.ReportSection
import io.github.zenhelix.dependanger.core.model.ReportSettings
import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.effective.model.EffectiveBundle
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectivePlugin
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitySeverity
import io.github.zenhelix.dependanger.features.updates.model.UpdateAvailableInfo
import io.github.zenhelix.dependanger.features.updates.model.UpdatesExtensionKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ReportGenerationTest {

    private val generator = ReportGenerator()

    private fun samplePlugin(
        alias: String = "kotlin-jvm",
        id: String = "org.jetbrains.kotlin.jvm",
        version: String = "2.1.20",
    ): EffectivePlugin = EffectivePlugin(
        alias = alias,
        id = id,
        version = ResolvedVersion(alias = "${alias}-version", value = version, source = VersionSource.DECLARED, originalRef = null),
    )

    private fun richMetadata(): EffectiveMetadata {
        val lib1 = sampleLibrary()
        val lib2 = sampleLibrary(
            alias = "guava",
            group = "com.google.guava",
            artifact = "guava",
            version = "33.0.0",
            tags = setOf("google", "collections"),
        )
        val deprecatedLib = sampleLibrary(
            alias = "old-lib",
            group = "com.example",
            artifact = "old-lib",
            version = "1.0.0",
            isDeprecated = true,
            deprecation = DeprecationInfo(
                replacedBy = "new-lib",
                message = "Use new-lib instead",
                since = "2.0.0",
                removalVersion = "3.0.0",
            ),
        )
        val plugin1 = samplePlugin()
        val bundle1 = EffectiveBundle(alias = "spring-web", libraries = listOf("spring-core", "spring-webmvc"))
        val version1 = ResolvedVersion(alias = "spring", value = "6.1.0", source = VersionSource.DECLARED, originalRef = null)

        return EffectiveMetadata(
            schemaVersion = "1.0",
            distribution = null,
            versions = mapOf("spring" to version1),
            libraries = mapOf(
                "spring-core" to lib1,
                "guava" to lib2,
                "old-lib" to deprecatedLib,
            ),
            plugins = mapOf("kotlin-jvm" to plugin1),
            bundles = mapOf("spring-web" to bundle1),
            diagnostics = Diagnostics.EMPTY,
            processingInfo = null,
        )
    }

    @Nested
    inner class `generating JSON reports` {

        @Test
        fun `produces valid JSON containing all expected sections`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.format).isEqualTo(ReportFormat.JSON)
            assertThat(report.content).contains("\"schemaVersion\"")
            assertThat(report.content).contains("\"summary\"")
            assertThat(report.content).contains("\"libraries\"")
            assertThat(report.content).contains("\"plugins\"")
            assertThat(report.content).contains("\"bundles\"")
            assertThat(report.content).contains("\"versions\"")
        }

        @Test
        fun `includes library coordinates in JSON output`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("org.springframework")
            assertThat(report.content).contains("spring-core")
            assertThat(report.content).contains("6.1.0")
        }

        @Test
        fun `JSON report output path is null when using generate`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.outputPath).isNull()
        }
    }

    @Nested
    inner class `generating YAML reports` {

        @Test
        fun `produces YAML with same data structure as JSON`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.YAML)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.format).isEqualTo(ReportFormat.YAML)
            assertThat(report.content).contains("schemaVersion")
            assertThat(report.content).contains("summary")
            assertThat(report.content).contains("libraries")
            assertThat(report.content).contains("plugins")
        }

        @Test
        fun `YAML report contains library data`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.YAML)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("spring-core")
            assertThat(report.content).contains("org.springframework")
        }
    }

    @Nested
    inner class `generating Markdown reports` {

        @Test
        fun `produces markdown with headers and tables`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.format).isEqualTo(ReportFormat.MARKDOWN)
            assertThat(report.content).contains("# Dependanger Report")
            assertThat(report.content).contains("## Summary")
            assertThat(report.content).contains("## Libraries")
            assertThat(report.content).contains("| Alias |")
        }

        @Test
        fun `markdown summary table shows correct counts`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("| Libraries | 3 |")
            assertThat(report.content).contains("| Plugins | 1 |")
            assertThat(report.content).contains("| Bundles | 1 |")
            assertThat(report.content).contains("| Deprecated | 1 |")
        }

        @Test
        fun `markdown lists plugins section`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("## Plugins")
            assertThat(report.content).contains("org.jetbrains.kotlin.jvm")
            assertThat(report.content).contains("2.1.20")
        }
    }

    @Nested
    inner class `generating HTML reports` {

        @Test
        fun `wraps markdown content in HTML template`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.HTML)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.format).isEqualTo(ReportFormat.HTML)
            assertThat(report.content).contains("<!DOCTYPE html>")
            assertThat(report.content).contains("<html")
            assertThat(report.content).contains("<head>")
            assertThat(report.content).contains("<body>")
            assertThat(report.content).contains("</html>")
        }

        @Test
        fun `HTML report includes style tag`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.HTML)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("<style>")
            assertThat(report.content).contains("</style>")
        }

        @Test
        fun `HTML report contains library data`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.HTML)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("spring-core")
            assertThat(report.content).contains("org.springframework")
        }
    }

    @Nested
    inner class `summary counts accuracy` {

        @Test
        fun `counts libraries plugins and bundles correctly`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"libraryCount\": 3")
            assertThat(report.content).contains("\"pluginCount\": 1")
            assertThat(report.content).contains("\"bundleCount\": 1")
        }

        @Test
        fun `counts deprecated libraries`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"deprecatedCount\": 1")
        }

        @Test
        fun `reports zero vulnerability count when no vulnerabilities present`() {
            val metadata = richMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"vulnerabilitiesCount\": 0")
        }
    }

    @Nested
    inner class `empty metadata handling` {

        @Test
        fun `produces report with zero counts for empty metadata`() {
            val metadata = emptyMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"libraryCount\": 0")
            assertThat(report.content).contains("\"pluginCount\": 0")
            assertThat(report.content).contains("\"bundleCount\": 0")
            assertThat(report.content).contains("\"deprecatedCount\": 0")
        }

        @Test
        fun `empty metadata markdown report still has header and summary`() {
            val metadata = emptyMetadata()
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("# Dependanger Report")
            assertThat(report.content).contains("## Summary")
            assertThat(report.content).contains("| Libraries | 0 |")
        }

        @Test
        fun `omits library section in JSON when no libraries present`() {
            val metadata = emptyMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            // encodeDefaults = false and explicitNulls = false means empty lists
            // still render as [] but null sections are omitted
            assertThat(report.content).contains("\"libraries\": []")
        }
    }

    @Nested
    inner class `vulnerability information in reports` {

        private fun metadataWithVulnerabilities(): EffectiveMetadata {
            val lib = sampleLibrary()
            val vulns = listOf(
                VulnerabilityInfo(
                    id = "GHSA-1234-abcd-efgh",
                    aliases = listOf("CVE-2024-12345"),
                    summary = "Remote code execution in spring-core",
                    severity = VulnerabilitySeverity.CRITICAL,
                    cvssScore = 9.8,
                    cvssVersion = "3.1",
                    fixedVersion = "6.1.1",
                    url = "https://github.com/advisories/GHSA-1234",
                    affectedGroup = "org.springframework",
                    affectedArtifact = "spring-core",
                    affectedVersion = "6.1.0",
                ),
                VulnerabilityInfo(
                    id = "GHSA-5678-ijkl-mnop",
                    aliases = listOf("CVE-2024-67890"),
                    summary = "Information disclosure",
                    severity = VulnerabilitySeverity.HIGH,
                    cvssScore = 7.5,
                    cvssVersion = "3.1",
                    fixedVersion = "6.1.2",
                    url = null,
                    affectedGroup = "org.springframework",
                    affectedArtifact = "spring-core",
                    affectedVersion = "6.1.0",
                ),
            )
            return EffectiveMetadata(
                schemaVersion = "1.0",
                distribution = null,
                versions = emptyMap(),
                libraries = mapOf("spring-core" to lib),
                plugins = emptyMap(),
                bundles = emptyMap(),
                diagnostics = Diagnostics.EMPTY,
                processingInfo = null,
            ).withExtension(VulnerabilitiesExtensionKey, vulns)
        }

        @Test
        fun `JSON report includes CVE identifiers`() {
            val metadata = metadataWithVulnerabilities()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("CVE-2024-12345")
            assertThat(report.content).contains("CVE-2024-67890")
        }

        @Test
        fun `report includes vulnerability severity`() {
            val metadata = metadataWithVulnerabilities()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("CRITICAL")
            assertThat(report.content).contains("HIGH")
        }

        @Test
        fun `report includes fixed version information`() {
            val metadata = metadataWithVulnerabilities()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("6.1.1")
            assertThat(report.content).contains("6.1.2")
        }

        @Test
        fun `summary counts vulnerabilities correctly`() {
            val metadata = metadataWithVulnerabilities()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"vulnerabilitiesCount\": 2")
            assertThat(report.content).contains("\"criticalVulnerabilities\": 1")
            assertThat(report.content).contains("\"highVulnerabilities\": 1")
        }
    }

    @Nested
    inner class `update information in reports` {

        private fun metadataWithUpdates(): EffectiveMetadata {
            val lib = sampleLibrary()
            val updates = listOf(
                UpdateAvailableInfo(
                    alias = "spring-core",
                    group = "org.springframework",
                    artifact = "spring-core",
                    currentVersion = "6.1.0",
                    latestVersion = "6.2.0",
                    updateType = UpdateType.MINOR,
                ),
            )
            return EffectiveMetadata(
                schemaVersion = "1.0",
                distribution = null,
                versions = emptyMap(),
                libraries = mapOf("spring-core" to lib),
                plugins = emptyMap(),
                bundles = emptyMap(),
                diagnostics = Diagnostics.EMPTY,
                processingInfo = null,
            ).withExtension(UpdatesExtensionKey, updates)
        }

        @Test
        fun `report includes current and available versions`() {
            val metadata = metadataWithUpdates()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"currentVersion\": \"6.1.0\"")
            assertThat(report.content).contains("\"availableVersion\": \"6.2.0\"")
        }

        @Test
        fun `report includes update type`() {
            val metadata = metadataWithUpdates()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"type\": \"MINOR\"")
        }

        @Test
        fun `summary shows updates available count`() {
            val metadata = metadataWithUpdates()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"updatesAvailable\": 1")
        }
    }

    @Nested
    inner class `distribution name in reports` {

        @Test
        fun `distribution name appears in JSON report`() {
            val metadata = emptyMetadata().copy(distribution = "enterprise")
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"distribution\": \"enterprise\"")
        }

        @Test
        fun `distribution name appears in markdown report`() {
            val metadata = emptyMetadata().copy(distribution = "enterprise")
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("Distribution: enterprise")
        }

        @Test
        fun `null distribution is omitted from JSON report`() {
            val metadata = emptyMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).doesNotContain("\"distribution\"")
        }
    }

    @Nested
    inner class `writing reports to filesystem` {

        @Test
        fun `generateToFile writes JSON file to disk`(@TempDir tempDir: Path) {
            val metadata = richMetadata()
            val settings = ReportSettings(
                format = ReportFormat.JSON,
                outputDir = tempDir.toString(),
                sections = ReportSection.entries,
            )

            val report = generator.generateToFile(metadata, settings, null)

            assertThat(report.outputPath).isNotNull()
            assertThat(report.outputPath!!.toFile()).exists()
            assertThat(report.outputPath!!.fileName.toString()).isEqualTo("dependanger-report.json")
            assertThat(report.outputPath!!.toFile().readText()).isEqualTo(report.content)
        }

        @Test
        fun `generateToFile writes Markdown file with correct extension`(@TempDir tempDir: Path) {
            val metadata = richMetadata()
            val settings = ReportSettings(
                format = ReportFormat.MARKDOWN,
                outputDir = tempDir.toString(),
                sections = ReportSection.entries,
            )

            val report = generator.generateToFile(metadata, settings, null)

            assertThat(report.outputPath!!.fileName.toString()).isEqualTo("dependanger-report.md")
        }

        @Test
        fun `generateToFile writes HTML file with correct extension`(@TempDir tempDir: Path) {
            val metadata = richMetadata()
            val settings = ReportSettings(
                format = ReportFormat.HTML,
                outputDir = tempDir.toString(),
                sections = ReportSection.entries,
            )

            val report = generator.generateToFile(metadata, settings, null)

            assertThat(report.outputPath!!.fileName.toString()).isEqualTo("dependanger-report.html")
        }

        @Test
        fun `generateToFile writes YAML file with correct extension`(@TempDir tempDir: Path) {
            val metadata = richMetadata()
            val settings = ReportSettings(
                format = ReportFormat.YAML,
                outputDir = tempDir.toString(),
                sections = ReportSection.entries,
            )

            val report = generator.generateToFile(metadata, settings, null)

            assertThat(report.outputPath!!.fileName.toString()).isEqualTo("dependanger-report.yaml")
        }

        @Test
        fun `generateToFile creates output directory if it does not exist`(@TempDir tempDir: Path) {
            val metadata = richMetadata()
            val nestedDir = tempDir.resolve("nested").resolve("deep")
            val settings = ReportSettings(
                format = ReportFormat.JSON,
                outputDir = nestedDir.toString(),
                sections = ReportSection.entries,
            )

            val report = generator.generateToFile(metadata, settings, null)

            assertThat(report.outputPath!!.toFile()).exists()
            assertThat(nestedDir.toFile()).isDirectory()
        }
    }

    @Nested
    inner class `report content matches across formats` {

        @Test
        fun `generate returns content with correct format field`() {
            val metadata = richMetadata()

            ReportFormat.entries.forEach { format ->
                val settings = allSectionsSettings(format)
                val report = generator.generate(metadata, settings, null)
                assertThat(report.format).isEqualTo(format)
            }
        }

        @Test
        fun `all formats include library names in output`() {
            val metadata = richMetadata()

            ReportFormat.entries.forEach { format ->
                val settings = allSectionsSettings(format)
                val report = generator.generate(metadata, settings, null)
                assertThat(report.content)
                    .`as`("Format $format should contain library name")
                    .contains("spring-core")
            }
        }
    }
}
