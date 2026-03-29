package io.github.zenhelix.dependanger.integration.generators

import io.github.zenhelix.dependanger.api.toToml
import io.github.zenhelix.dependanger.api.writeTomlTo
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TomlGenerationE2ETest : IntegrationTestBase() {

    @Test
    fun `generates TOML with all sections from standard catalog`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            versions {
                version("kotlin", "2.1.20")
                version("ktor", "3.1.1")
            }
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
            }
            plugins {
                plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin"))
            }
            bundles {
                bundle("ktor") { libraries("ktor-core", "ktor-cio") }
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val toml = result.toToml()
        assertThat(toml).contains("[versions]")
        assertThat(toml).contains("[libraries]")
        assertThat(toml).contains("[plugins]")
        assertThat(toml).contains("[bundles]")
    }

    @Test
    fun `versions section contains all declared versions`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            versions {
                version("kotlin", "2.1.20")
                version("ktor", "3.1.1")
                version("assertj", "3.27.3")
            }
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                library("assertj", "org.assertj:assertj-core", versionRef("assertj"))
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val toml = result.toToml()
        assertThat(toml).contains("kotlin = \"2.1.20\"")
        assertThat(toml).contains("ktor = \"3.1.1\"")
        assertThat(toml).contains("assertj = \"3.27.3\"")
    }

    @Test
    fun `libraries section uses version refs`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            versions { version("kotlin", "2.1.20") }
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val toml = result.toToml()
        assertThat(toml).contains("version.ref = \"kotlin\"")
    }

    @Test
    fun `bundles section lists member libraries`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            versions { version("ktor", "3.1.1") }
            libraries {
                library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
                library("ktor-json", "io.ktor:ktor-serialization-json", versionRef("ktor"))
            }
            bundles {
                bundle("ktor") { libraries("ktor-core", "ktor-cio", "ktor-json") }
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val toml = result.toToml()
        assertThat(toml).contains("[bundles]")
        assertThat(toml).contains("\"ktor-core\"")
        assertThat(toml).contains("\"ktor-cio\"")
        assertThat(toml).contains("\"ktor-json\"")
    }

    @Test
    fun `plugins section with version refs`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            versions { version("kotlin", "2.1.20") }
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            }
            plugins {
                plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin"))
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val toml = result.toToml()
        assertThat(toml).contains("[plugins]")
        assertThat(toml).contains("kotlin-jvm = { id = \"org.jetbrains.kotlin.jvm\", version.ref = \"kotlin\" }")
    }

    @Test
    fun `distribution filtering produces subset TOML`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
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
        val toml = result.toToml()
        assertThat(toml).contains("android-lib")
        assertThat(toml).contains("common-lib")
        assertThat(toml).doesNotContain("server-lib")
    }

    @Test
    fun `deprecated library includes comment when configured`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            libraries {
                library("old-lib", "com.example:old-lib:1.0.0") {
                    deprecated(replacedBy = "new-lib", message = "Use new-lib instead")
                }
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val toml = result.toToml(
            TomlConfig(
                filename = "libs.versions.toml",
                includeComments = true,
                sortSections = true,
                useInlineVersions = false,
                includeDeprecationComments = true,
            )
        )
        assertThat(toml).contains("DEPRECATED")
        assertThat(toml).contains("Use new-lib instead")
    }

    @Test
    fun `sorted sections when configured`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            versions {
                version("ktor", "3.1.1")
                version("assertj", "3.27.3")
                version("kotlin", "2.1.20")
            }
            libraries {
                library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                library("assertj", "org.assertj:assertj-core", versionRef("assertj"))
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val toml = result.toToml(
            TomlConfig(
                filename = "libs.versions.toml",
                includeComments = true,
                sortSections = true,
                useInlineVersions = false,
                includeDeprecationComments = true,
            )
        )

        val lines = toml.lines()
        val versionLines = lines
            .dropWhile { it != "[versions]" }.drop(1)
            .takeWhile { it.isNotBlank() && !it.startsWith("[") }
        val versionAliases = versionLines.map { it.substringBefore(" =") }
        assertThat(versionAliases).isSorted()
    }

    @Test
    fun `inline versions when configured`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            versions { version("kotlin", "2.1.20") }
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val toml = result.toToml(
            TomlConfig(
                filename = "libs.versions.toml",
                includeComments = true,
                sortSections = true,
                useInlineVersions = true,
                includeDeprecationComments = true,
            )
        )
        assertThat(toml).contains("version = \"2.1.20\"")
        assertThat(toml).doesNotContain("version.ref")
    }

    @Test
    fun `writeTomlTo writes file to disk`(@TempDir tempDir: Path) = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            versions { version("kotlin", "2.1.20") }
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            }
        }.process()

        assertThat(result.isSuccess).isTrue()

        result.writeTomlTo(tempDir)

        val outputFile = tempDir.resolve("libs.versions.toml")
        assertThat(outputFile).exists()
        val content = outputFile.toFile().readText()
        assertThat(content).contains("[versions]")
        assertThat(content).contains("[libraries]")
        assertThat(content).contains("kotlin-stdlib")
    }
}
