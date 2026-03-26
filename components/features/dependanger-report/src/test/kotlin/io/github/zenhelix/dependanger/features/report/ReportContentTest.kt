package io.github.zenhelix.dependanger.features.report

import io.github.zenhelix.dependanger.core.model.DeprecationInfo
import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.ReportFormat
import io.github.zenhelix.dependanger.core.model.Severity
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ReportContentTest {

    private val generator = ReportGenerator()

    @Nested
    inner class `JSON report validity` {

        @Test
        fun `JSON report is parseable by kotlinx serialization`() {
            val lib = sampleLibrary()
            val metadata = emptyMetadata().copy(
                libraries = mapOf("spring-core" to lib),
            )
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)
            val parsed = Json.parseToJsonElement(report.content)

            assertThat(parsed).isInstanceOf(JsonObject::class.java)
        }

        @Test
        fun `JSON report contains summary as nested object`() {
            val metadata = emptyMetadata()
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)
            val parsed = Json.parseToJsonElement(report.content) as JsonObject

            assertThat(parsed.containsKey("summary")).isTrue()
            assertThat(parsed["summary"]).isInstanceOf(JsonObject::class.java)
        }

        @Test
        fun `JSON report with rich data is still valid JSON`() {
            val lib1 = sampleLibrary()
            val lib2 = sampleLibrary(alias = "guava", group = "com.google.guava", artifact = "guava", version = "33.0.0")
            val plugin = EffectivePlugin(
                alias = "kotlin-jvm",
                id = "org.jetbrains.kotlin.jvm",
                version = ResolvedVersion(alias = "kotlin", value = "2.1.20", source = VersionSource.DECLARED, originalRef = null),
            )
            val bundle = EffectiveBundle(alias = "spring-web", libraries = listOf("spring-core", "spring-webmvc"))
            val version = ResolvedVersion(alias = "spring", value = "6.1.0", source = VersionSource.DECLARED, originalRef = null)

            val metadata = EffectiveMetadata(
                schemaVersion = "1.0",
                distribution = "enterprise",
                versions = mapOf("spring" to version),
                libraries = mapOf("spring-core" to lib1, "guava" to lib2),
                plugins = mapOf("kotlin-jvm" to plugin),
                bundles = mapOf("spring-web" to bundle),
                diagnostics = Diagnostics.EMPTY,
                processingInfo = null,
            )
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)
            val parsed = Json.parseToJsonElement(report.content) as JsonObject

            assertThat(parsed.containsKey("libraries")).isTrue()
            assertThat(parsed.containsKey("plugins")).isTrue()
            assertThat(parsed.containsKey("bundles")).isTrue()
            assertThat(parsed.containsKey("versions")).isTrue()
        }
    }

    @Nested
    inner class `Markdown table structure` {

        @Test
        fun `libraries table has four columns`() {
            val lib = sampleLibrary()
            val metadata = emptyMetadata().copy(libraries = mapOf("spring-core" to lib))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)
            val libraryHeaderLine = report.content.lines().first { it.contains("| Alias |") && it.contains("Coordinates") }

            val columnCount = libraryHeaderLine.count { it == '|' } - 1
            assertThat(columnCount).isEqualTo(4)
        }

        @Test
        fun `plugins table has three columns`() {
            val plugin = EffectivePlugin(
                alias = "kotlin-jvm",
                id = "org.jetbrains.kotlin.jvm",
                version = ResolvedVersion(alias = "kotlin", value = "2.1.20", source = VersionSource.DECLARED, originalRef = null),
            )
            val metadata = emptyMetadata().copy(plugins = mapOf("kotlin-jvm" to plugin))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)
            val pluginHeaderLine = report.content.lines().first { it.contains("| Alias |") && it.contains("Plugin ID") }

            val columnCount = pluginHeaderLine.count { it == '|' } - 1
            assertThat(columnCount).isEqualTo(3)
        }

        @Test
        fun `versions table has three columns`() {
            val version = ResolvedVersion(alias = "spring", value = "6.1.0", source = VersionSource.DECLARED, originalRef = null)
            val metadata = emptyMetadata().copy(versions = mapOf("spring" to version))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)
            val versionHeaderLine = report.content.lines().first { it.contains("| Alias |") && it.contains("Fallback") }

            val columnCount = versionHeaderLine.count { it == '|' } - 1
            assertThat(columnCount).isEqualTo(3)
        }
    }

    @Nested
    inner class `Markdown vulnerability section` {

        private val report = run {
            val lib = sampleLibrary()
            val vuln = VulnerabilityInfo(
                id = "GHSA-1234-abcd-efgh",
                aliases = listOf("CVE-2024-12345"),
                summary = "Remote code execution",
                severity = VulnerabilitySeverity.CRITICAL,
                cvssScore = 9.8,
                cvssVersion = "3.1",
                fixedVersion = "6.1.1",
                url = null,
                affectedGroup = "org.springframework",
                affectedArtifact = "spring-core",
                affectedVersion = "6.1.0",
            )
            val metadata = emptyMetadata().copy(
                libraries = mapOf("spring-core" to lib),
            ).withExtension(VulnerabilitiesExtensionKey, listOf(vuln))
            generator.generate(metadata, allSectionsSettings(ReportFormat.MARKDOWN), null)
        }

        @Test
        fun `vulnerability section shows CVSS scores`() {
            assertThat(report.content).contains("9.8")
        }

        @Test
        fun `vulnerability section shows CVE identifier`() {
            assertThat(report.content).contains("CVE-2024-12345")
        }

        @Test
        fun `vulnerability table has six columns`() {
            val vulnHeaderLine = report.content.lines().first { it.contains("| Library |") && it.contains("CVE") }

            val columnCount = vulnHeaderLine.count { it == '|' } - 1
            assertThat(columnCount).isEqualTo(6)
        }

        @Test
        fun `vulnerability section includes severity and fixed version`() {
            assertThat(report.content).contains("CRITICAL")
            assertThat(report.content).contains("6.1.1")
        }
    }

    @Nested
    inner class `HTML report structure` {

        private val report = generator.generate(emptyMetadata(), allSectionsSettings(ReportFormat.HTML), null)

        @Test
        fun `HTML report has DOCTYPE declaration`() {
            assertThat(report.content).startsWith("<!DOCTYPE html>")
        }

        @Test
        fun `HTML report has head body and style tags`() {
            assertThat(report.content).contains("<head>")
            assertThat(report.content).contains("</head>")
            assertThat(report.content).contains("<body>")
            assertThat(report.content).contains("</body>")
            assertThat(report.content).contains("<style>")
            assertThat(report.content).contains("</style>")
        }

        @Test
        fun `HTML report has footer with generation timestamp`() {
            assertThat(report.content).contains("<footer>")
            assertThat(report.content).contains("Generated by Dependanger at")
        }

        @Test
        fun `HTML report has title tag`() {
            assertThat(report.content).contains("<title>Dependanger Report</title>")
        }
    }

    @Nested
    inner class `deprecated libraries section` {

        @Test
        fun `deprecated section appears when deprecated libraries exist`() {
            val lib = sampleLibrary(
                alias = "old-lib",
                group = "com.example",
                artifact = "old-lib",
                isDeprecated = true,
                deprecation = DeprecationInfo(
                    replacedBy = "new-lib",
                    message = "Superseded by new-lib",
                    since = "2.0.0",
                    removalVersion = null,
                ),
            )
            val metadata = emptyMetadata().copy(libraries = mapOf("old-lib" to lib))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("## Deprecated Libraries")
            assertThat(report.content).contains("old-lib")
            assertThat(report.content).contains("Superseded by new-lib")
            assertThat(report.content).contains("new-lib")
        }

        @Test
        fun `deprecated section is omitted when no deprecated libraries exist`() {
            val lib = sampleLibrary()
            val metadata = emptyMetadata().copy(libraries = mapOf("spring-core" to lib))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).doesNotContain("## Deprecated Libraries")
        }
    }

    @Nested
    inner class `empty sections are omitted` {

        @Test
        fun `updates section is omitted when no updates exist`() {
            val lib = sampleLibrary()
            val metadata = emptyMetadata().copy(libraries = mapOf("spring-core" to lib))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).doesNotContain("## Available Updates")
        }

        @Test
        fun `vulnerabilities section is omitted when no vulnerabilities exist`() {
            val lib = sampleLibrary()
            val metadata = emptyMetadata().copy(libraries = mapOf("spring-core" to lib))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).doesNotContain("## Vulnerabilities")
        }

        @Test
        fun `updates appear in summary only when present`() {
            val lib = sampleLibrary()
            val metadata = emptyMetadata().copy(libraries = mapOf("spring-core" to lib))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).doesNotContain("Updates Available")
        }

        @Test
        fun `updates section appears when updates exist`() {
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
            val metadata = emptyMetadata().copy(
                libraries = mapOf("spring-core" to lib),
            ).withExtension(UpdatesExtensionKey, updates)
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("## Available Updates")
            assertThat(report.content).contains("6.2.0")
        }

        @Test
        fun `vulnerability count is omitted from summary when zero`() {
            val metadata = emptyMetadata()
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).doesNotContain("| Vulnerabilities |")
        }
    }

    @Nested
    inner class `special characters in markdown` {

        @Test
        fun `pipe characters in library names are escaped`() {
            val lib = sampleLibrary(
                alias = "lib-with|pipe",
                group = "com.example",
                artifact = "lib-with-pipe",
            )
            val metadata = emptyMetadata().copy(libraries = mapOf("lib-with|pipe" to lib))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("lib-with\\|pipe")
        }

        @Test
        fun `newlines in content are replaced with spaces in table cells`() {
            val lib = sampleLibrary(
                alias = "lib-with\nnewline",
                group = "com.example",
                artifact = "lib-artifact",
            )
            val metadata = emptyMetadata().copy(libraries = mapOf("lib-with\nnewline" to lib))
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            // The escapeCell function replaces newlines with spaces
            val libraryTableLines = report.content.lines().filter { it.startsWith("|") && it.contains("com.example") }
            assertThat(libraryTableLines).allSatisfy { line ->
                // Each table row should be a single line
                assertThat(line).doesNotContain("\n")
            }
        }
    }

    @Nested
    inner class `validation section rendering` {

        @Test
        fun `validation section shows errors when present`() {
            val diagnostics = Diagnostics(
                errors = listOf(
                    DiagnosticMessage(
                        code = "UNRESOLVED_VERSION",
                        message = "Version not found for lib-x",
                        severity = Severity.ERROR,
                        processorId = "validation",
                        context = emptyMap(),
                    ),
                ),
                warnings = emptyList(),
                infos = emptyList(),
            )
            val metadata = emptyMetadata().copy(diagnostics = diagnostics)
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("## Validation")
            assertThat(report.content).contains("### Errors")
            assertThat(report.content).contains("UNRESOLVED_VERSION")
            assertThat(report.content).contains("Version not found for lib-x")
        }

        @Test
        fun `validation section shows warnings when present`() {
            val diagnostics = Diagnostics(
                errors = emptyList(),
                warnings = listOf(
                    DiagnosticMessage(
                        code = "DEPRECATED_LIBRARY",
                        message = "Library old-lib is deprecated",
                        severity = Severity.WARNING,
                        processorId = "validation",
                        context = emptyMap(),
                    ),
                ),
                infos = emptyList(),
            )
            val metadata = emptyMetadata().copy(diagnostics = diagnostics)
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("### Warnings")
            assertThat(report.content).contains("DEPRECATED_LIBRARY")
        }

        @Test
        fun `validation section says all checks passed when no errors or warnings`() {
            val metadata = emptyMetadata()
            val settings = allSectionsSettings(ReportFormat.MARKDOWN)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("All checks passed.")
        }

        @Test
        fun `summary counts validation errors and warnings`() {
            val diagnostics = Diagnostics(
                errors = listOf(
                    DiagnosticMessage("E1", "Error 1", Severity.ERROR, null, emptyMap()),
                    DiagnosticMessage("E2", "Error 2", Severity.ERROR, null, emptyMap()),
                ),
                warnings = listOf(
                    DiagnosticMessage("W1", "Warning 1", Severity.WARNING, null, emptyMap()),
                ),
                infos = emptyList(),
            )
            val metadata = emptyMetadata().copy(diagnostics = diagnostics)
            val settings = allSectionsSettings(ReportFormat.JSON)

            val report = generator.generate(metadata, settings, null)

            assertThat(report.content).contains("\"validationErrors\": 2")
            assertThat(report.content).contains("\"validationWarnings\": 1")
        }
    }
}
