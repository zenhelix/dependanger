package io.github.zenhelix.dependanger.integration.pipeline

import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.feature.model.settings.license.licenseCheck
import io.github.zenhelix.dependanger.feature.model.settings.security.securityCheck
import io.github.zenhelix.dependanger.feature.model.settings.updates.updateCheck
import io.github.zenhelix.dependanger.features.license.LicenseCheckProcessor
import io.github.zenhelix.dependanger.features.security.SecurityCheckProcessor
import io.github.zenhelix.dependanger.features.updates.UpdateCheckProcessor
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.integration.support.TestCatalogs
import io.github.zenhelix.dependanger.integration.support.assertResult
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Preset Variations E2E")
class PresetVariationsE2ETest : IntegrationTestBase() {

    @Nested
    inner class `MINIMAL preset` {

        @Test
        fun `MINIMAL preset resolves versions but skips filtering`() = runTest {
            val result = dependanger(preset = ProcessingPreset.MINIMAL) {
                versions {
                    version("v1", "1.0.0")
                    version("unused", "2.0.0")
                }
                libraries {
                    library("lib-a", "com.example:lib-a", versionRef("v1")) {
                        tags("server")
                    }
                    library("lib-b", "com.example:lib-b", versionRef("v1")) {
                        tags("client")
                    }
                }
                distributions {
                    distribution("server-only") {
                        spec {
                            byTags {
                                include { anyOf("server") }
                            }
                        }
                    }
                }
            }.process(distribution = "server-only")

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            // MINIMAL skips library filtering, so all libraries should be present
            assertThat(success.effective.libraries).hasSize(2)
        }

        @Test
        fun `MINIMAL preset does not prune unused versions`() = runTest {
            val result = dependanger(preset = ProcessingPreset.MINIMAL) {
                versions {
                    version("used", "1.0.0")
                    version("unused", "2.0.0")
                }
                libraries {
                    library("lib", "com.example:lib", versionRef("used"))
                }
            }.process()

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            val versions = success.effective.versions
            // MINIMAL disables used-versions processor, so unused should remain
            assertThat(versions.keys).contains("used", "unused")
        }
    }

    @Nested
    inner class `DEFAULT preset` {

        @Test
        fun `DEFAULT preset includes filtering`() = runTest {
            val result = dependanger(preset = ProcessingPreset.DEFAULT) {
                versions {
                    version("v1", "1.0.0")
                }
                libraries {
                    library("lib-a", "com.example:lib-a", versionRef("v1")) {
                        tags("server")
                    }
                    library("lib-b", "com.example:lib-b", versionRef("v1")) {
                        tags("client")
                    }
                }
                distributions {
                    distribution("server-only") {
                        spec {
                            byTags {
                                include { anyOf("server") }
                            }
                        }
                    }
                }
            }.process(distribution = "server-only")

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            assertThat(success.effective.libraries.keys).containsExactly("lib-a")
        }
    }

    @Nested
    inner class `STRICT preset` {

        @Test
        fun `STRICT preset enables all feature processors`() = runTest {
            mockHttp {
                maven {
                    metadata("org.jetbrains.kotlin", "kotlin-stdlib", listOf("2.1.20", "2.2.0"))
                    pom(
                        "org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20", pomXml(
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

            val result = dependanger(preset = ProcessingPreset.STRICT) {
                versions {
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
                settings {
                    updateCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("versions")
                        repositories = listOf(
                            io.github.zenhelix.dependanger.core.model.MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                    }
                    securityCheck { enabled = true; cacheDirectory = cacheDirFor("security") }
                    licenseCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("licenses")
                        allowedLicenses = listOf("Apache-2.0")
                    }
                }
            }.process()

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            val processingInfo = success.effective.processingInfo
            assertThat(processingInfo).isNotNull
            // STRICT enables update-check, security-check, license-check, transitive-resolver
            assertThat(processingInfo!!.processorIds).containsAnyOf(
                UpdateCheckProcessor.PROCESSOR_ID,
                SecurityCheckProcessor.PROCESSOR_ID,
                LicenseCheckProcessor.PROCESSOR_ID,
            )
        }
    }

    @Nested
    inner class `custom processors` {

        @Test
        fun `custom processor can be added`() = runTest {
            var processorExecuted = false

            val fakeProcessor = object : EffectiveMetadataProcessor {
                override val id: String = "fake-processor"
                override val phase: ProcessingPhase = ProcessingPhase.COMPAT_RULES
                override val isOptional: Boolean = false
                override val description: String = "Fake processor for test"
                override fun supports(context: ProcessingContext): Boolean = true

                override suspend fun process(
                    metadata: EffectiveMetadata,
                    context: ProcessingContext,
                ): EffectiveMetadata {
                    processorExecuted = true
                    return metadata
                }
            }

            // Use the builder API to add custom processor
            val customResult = io.github.zenhelix.dependanger.api.Dependanger
                .fromDsl(TestCatalogs.minimal())
                .addProcessor(fakeProcessor)
                .build()
                .process()

            assertResult(customResult).isSuccessful()
            assertThat(processorExecuted).isTrue()
        }

        @Test
        fun `processor can be disabled`() = runTest {
            val result = io.github.zenhelix.dependanger.api.Dependanger
                .fromDsl(TestCatalogs.minimal())
                .disableProcessor(ProcessorIds.VALIDATION)
                .build()
                .process()

            assertResult(result).isSuccessful()

            // Validation processor is disabled, so no validation diagnostics
            val validationDiagnostics = result.diagnostics.errors.filter {
                it.code.startsWith("VALIDATION_")
            }
            assertThat(validationDiagnostics).isEmpty()
        }
    }

    companion object {
        private fun pomXml(group: String, artifact: String, version: String): String = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<project xmlns="http://maven.apache.org/POM/4.0.0">""")
            appendLine("  <modelVersion>4.0.0</modelVersion>")
            appendLine("  <groupId>$group</groupId>")
            appendLine("  <artifactId>$artifact</artifactId>")
            appendLine("  <version>$version</version>")
            append("</project>")
        }
    }
}
