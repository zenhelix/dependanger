package io.github.zenhelix.dependanger.integration.pipeline

import io.github.zenhelix.dependanger.api.toBom
import io.github.zenhelix.dependanger.api.toToml
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.integration.support.MavenResponses
import io.github.zenhelix.dependanger.integration.support.TestCatalogs
import io.github.zenhelix.dependanger.integration.support.assertBom
import io.github.zenhelix.dependanger.integration.support.assertResult
import io.github.zenhelix.dependanger.integration.support.assertToml
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Full Pipeline E2E")
class FullPipelineE2ETest : IntegrationTestBase() {

    @Nested
    inner class `minimal catalog` {

        @Test
        fun `minimal catalog produces valid effective metadata`() = runTest {
            val result = dependanger(dslBlock = TestCatalogs.minimal()).process()

            assertResult(result)
                .isSuccessful()
                .hasLibraryCount(2)

            val effective = result.effective!!
            assertThat(effective.versions).isNotEmpty
            assertThat(effective.libraries).hasSize(2)
            assertThat(effective.plugins).hasSize(1)
            assertThat(effective.bundles).hasSize(1)
        }
    }

    @Nested
    inner class `standard catalog` {

        @Test
        fun `standard catalog produces TOML with all sections`() = runTest {
            val result = dependanger(dslBlock = TestCatalogs.standard()).process()

            assertResult(result).isSuccessful()

            val toml = result.toToml()
            assertToml(toml)
                .hasVersionsSection()
                .hasLibrariesSection()
                .hasBundlesSection()
                .hasPluginsSection()
        }

        @Test
        fun `standard catalog produces valid BOM XML`() = runTest {
            val result = dependanger(dslBlock = TestCatalogs.standard()).process()

            assertResult(result).isSuccessful()

            val bom = result.toBom(bomConfig())
            assertBom(bom)
                .hasGroupId("io.test")
                .hasArtifactId("test-bom")
            assertThat(bom).contains("<version>1.0.0</version>")
        }
    }

    @Nested
    inner class `complex catalog` {

        @Test
        fun `complex catalog with all features enabled`() = runTest {
            mockHttp {
                maven {
                    metadata("org.jetbrains.kotlin", "kotlin-stdlib", listOf("2.1.20", "2.2.0"))
                    pom(
                        "org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20", MavenResponses.pomXml(
                            "org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20"
                        )
                    )
                }
                osv {
                    noVulnerabilities("org.jetbrains.kotlin:kotlin-stdlib")
                }
                clearlyDefined {
                    license("org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20", "Apache-2.0")
                }
            }

            val result = dependanger(preset = ProcessingPreset.STRICT, dslBlock = TestCatalogs.complex()).process()

            assertResult(result).isSuccessful()

            val effective = result.effective!!
            assertThat(effective.libraries).isNotEmpty
            assertThat(effective.versions).isNotEmpty
        }
    }

    @Nested
    inner class `empty catalog` {

        @Test
        fun `empty DSL produces valid empty result`() = runTest {
            val result = dependanger { }.process()

            assertResult(result).isSuccessful()

            val effective = result.effective!!
            assertThat(effective.libraries).isEmpty()
            assertThat(effective.versions).isEmpty()
            assertThat(effective.plugins).isEmpty()
            assertThat(effective.bundles).isEmpty()
        }
    }

    @Nested
    inner class `processing info` {

        @Test
        fun `processing returns diagnostics info`() = runTest {
            val result = dependanger(dslBlock = TestCatalogs.minimal()).process()

            assertResult(result).isSuccessful()

            val processingInfo = result.effective!!.processingInfo
            assertThat(processingInfo).isNotNull
            assertThat(processingInfo!!.processorIds).isNotEmpty
            assertThat(processingInfo.processedAt).isNotBlank()
        }
    }

    @Nested
    inner class `round-trip consistency` {

        @Test
        fun `process then generate TOML then verify round-trip consistency`() = runTest {
            val result = dependanger {
                versions {
                    version("kotlin", "2.1.20")
                    version("ktor", "3.0.0")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                }
                plugins {
                    plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin"))
                }
            }.process()

            assertResult(result).isSuccessful()

            val toml = result.toToml()

            assertToml(toml)
                .containsVersion("kotlin", "2.1.20")
                .containsVersion("ktor", "3.0.0")
                .containsLibrary("kotlin-stdlib")
                .containsLibrary("ktor-core")
                .containsPlugin("kotlin-jvm")
        }

        @Test
        fun `process then generate BOM then verify all libraries present`() = runTest {
            val result = dependanger {
                versions {
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                    library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect", versionRef("kotlin"))
                }
            }.process()

            assertResult(result).isSuccessful()

            val bom = result.toBom(bomConfig())

            assertBom(bom)
                .containsDependency("org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20")
                .containsDependency("org.jetbrains.kotlin", "kotlin-reflect", "2.1.20")
        }
    }

}
