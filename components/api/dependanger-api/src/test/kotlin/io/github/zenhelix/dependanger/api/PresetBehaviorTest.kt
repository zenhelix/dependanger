package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PresetBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) { preset(preset) }

    @Nested
    inner class `DEFAULT preset` {

        @Test
        fun `default preset processes core pipeline successfully`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("kotlin-stdlib")
            assertThat((result as DependangerResult.Success).effective.libraries["kotlin-stdlib"]!!.version!!.value).isEqualTo("2.1.20")
        }

        @Test
        fun `default preset does not run feature processors`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib", "com.example:lib:1.0.0")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).isEmpty()
            assertThat(result.vulnerabilities).isEmpty()
            assertThat(result.licenseViolations).isEmpty()
            assertThat(result.transitives).isEmpty()
            assertThat(result.compatibilityIssues).isEmpty()
            assertThat(result.versionConflicts).isEmpty()
        }

        @Test
        fun `default preset resolves versions and filters`() = runTest {
            val result = dependanger {
                versions {
                    version("ktor", "3.1.1")
                    version("unused", "9.9.9")
                }
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                    library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.versions).containsKey("ktor")
            assertThat((result as DependangerResult.Success).effective.versions).doesNotContainKey("unused")
            assertThat((result as DependangerResult.Success).effective.libraries["ktor-core"]!!.version!!.value).isEqualTo("3.1.1")
            assertThat((result as DependangerResult.Success).effective.libraries["ktor-cio"]!!.version!!.value).isEqualTo("3.1.1")
        }
    }

    @Nested
    inner class `MINIMAL preset` {

        @Test
        fun `minimal preset produces valid effective metadata`() = runTest {
            val result = dependanger(ProcessingPreset.MINIMAL) {
                libraries {
                    library("lib", "com.example:lib:1.0.0")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result).isInstanceOf(DependangerResult.Success::class.java)
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("lib")
        }

        @Test
        fun `minimal preset skips validation diagnostics`() = runTest {
            val result = dependanger(ProcessingPreset.MINIMAL) {
                libraries {
                    library("lib-a", "com.example:lib-a:1.0.0")
                    library("lib-b", "com.example:lib-b:2.0.0")
                }
                bundles {
                    bundle("test-bundle") { libraries("lib-a", "lib-b", "nonexistent-lib") }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val validationErrors = result.diagnostics.errors.filter {
                it.code == DiagnosticCodes.Validation.BUNDLE_REF_MISSING
            }
            assertThat(validationErrors).isEmpty()
        }

        @Test
        fun `minimal preset still converts metadata to effective`() = runTest {
            val result = dependanger(ProcessingPreset.MINIMAL) {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                    library("assertj", "org.assertj:assertj-core:3.27.3")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(2)
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("kotlin-stdlib")
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("assertj")
        }
    }

    @Nested
    inner class `STRICT preset` {

        @Test
        fun `strict preset adds validation errors for issues`() = runTest {
            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("broken-lib", "com.example:broken", versionRef("nonexistent"))
                }
            }.process()

            assertThat(result.isSuccess).isFalse()
            assertThat(result.diagnostics.errors).anyMatch {
                it.code == DiagnosticCodes.Version.UNRESOLVED
            }
        }

        @Test
        fun `strict preset treats warnings as errors`() = runTest {
            val dslBlock: DependangerDsl.() -> Unit = {
                libraries {
                    library("lib", "com.example:lib:1.0.0")
                }
                bundles {
                    bundle("test-bundle") { libraries("lib", "missing-lib") }
                }
            }

            val defaultResult = dependanger(ProcessingPreset.DEFAULT, dslBlock).process()
            val strictResult = dependanger(ProcessingPreset.STRICT, dslBlock).process()

            val defaultDiagnosticCount = defaultResult.diagnostics.errors.size +
                    defaultResult.diagnostics.warnings.size + defaultResult.diagnostics.infos.size
            val strictDiagnosticCount = strictResult.diagnostics.errors.size +
                    strictResult.diagnostics.warnings.size + strictResult.diagnostics.infos.size

            assertThat(strictDiagnosticCount).isGreaterThanOrEqualTo(defaultDiagnosticCount)
        }
    }

    @Nested
    inner class `Distribution filtering` {

        @Test
        fun `without distribution parameter processes all libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib", "com.example:lib:1.0.0") { tags("server") }
                }
                distributions {
                    distribution("server") {
                        spec { byTags { include { anyOf("server") } } }
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(1)
        }

        @Test
        fun `with distribution parameter filters libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("android-lib", "com.android:lib:1.0") { tags("android") }
                    library("server-lib", "com.server:lib:2.0") { tags("server") }
                }
                distributions {
                    distribution("android") {
                        spec { byTags { include { anyOf("android") } } }
                    }
                }
            }.process(distribution = "android")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("android-lib")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("server-lib")
        }

        @Test
        fun `filters libraries by distribution tags`() = runTest {
            val result = dependanger {
                versions { version("ktor", "3.1.1") }
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) { tags("jvm") }
                    library("ktor-js", "io.ktor:ktor-client-js", versionRef("ktor")) { tags("js") }
                    library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor")) { tags("jvm") }
                }
                distributions {
                    distribution("jvm") {
                        spec { byTags { include { anyOf("jvm") } } }
                    }
                }
            }.process(distribution = "jvm")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("ktor-core", "ktor-cio")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("ktor-js")
            assertThat((result as DependangerResult.Success).effective.libraries["ktor-core"]!!.version!!.value).isEqualTo("3.1.1")
        }
    }

    @Nested
    inner class `Preset with custom processors` {

        private val testExtensionKey = ExtensionKey("test-custom-extension", String.serializer())

        @Test
        fun `adding processor to default preset extends pipeline`() = runTest {
            val customProcessor = FakeProcessor(
                id = "custom-enrichment",
                phase = ProcessingPhase.VALIDATION,
                constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)),
                extensionKey = testExtensionKey,
                provider = { metadata -> "processed ${metadata.libraries.size} libraries" },
            )

            val result = Dependanger({
                                         libraries {
                                             library("lib-a", "com.a:lib:1.0.0")
                                             library("lib-b", "com.b:lib:2.0.0")
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(customProcessor)
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(2)
            val extensionValue = (result as DependangerResult.Success).effective.extensions[testExtensionKey]
            assertThat(extensionValue).isEqualTo("processed 2 libraries")
        }

        @Test
        fun `disabling processor removes it from pipeline`() = runTest {
            val dslBlock: DependangerDsl.() -> Unit = {
                versions {
                    version("used", "1.0.0")
                    version("unused", "2.0.0")
                }
                libraries {
                    library("lib", "com.example:lib", versionRef("used"))
                }
            }

            val defaultResult = dependanger(ProcessingPreset.DEFAULT, dslBlock).process()

            assertThat(defaultResult.isSuccess).isTrue()
            assertThat((defaultResult as DependangerResult.Success).effective.versions).doesNotContainKey("unused")

            val disabledResult = Dependanger(dslBlock) {
                preset(ProcessingPreset.DEFAULT)
                disableProcessor("used-versions")
            }.process()

            assertThat(disabledResult.isSuccess).isTrue()
            assertThat((disabledResult as DependangerResult.Success).effective.versions).containsKey("unused")
        }
    }

    @Nested
    inner class `Preset switching` {

        @Test
        fun `different presets produce different results for same input`() = runTest {
            val dslBlock: DependangerDsl.() -> Unit = {
                versions {
                    version("ktor", "3.1.1")
                    version("unused", "9.9.9")
                }
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                }
            }

            val defaultResult = dependanger(ProcessingPreset.DEFAULT, dslBlock).process()
            val minimalResult = dependanger(ProcessingPreset.MINIMAL, dslBlock).process()

            assertThat(defaultResult.isSuccess).isTrue()
            assertThat(minimalResult.isSuccess).isTrue()

            assertThat((defaultResult as DependangerResult.Success).effective.versions).doesNotContainKey("unused")
            assertThat((minimalResult as DependangerResult.Success).effective.versions).containsKey("unused")
        }
    }
}
