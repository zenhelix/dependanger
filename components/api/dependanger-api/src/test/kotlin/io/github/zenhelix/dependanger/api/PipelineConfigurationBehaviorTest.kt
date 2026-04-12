package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.pipeline.ExecutionMode
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val TEST_PHASE_A = ProcessingPhase("TEST_PHASE_A", ExecutionMode.PARALLEL_IO)
private val TEST_PHASE_B = ProcessingPhase("TEST_PHASE_B", ExecutionMode.PARALLEL_IO)

class PipelineConfigurationBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) { preset(preset) }

    @Nested
    inner class `Disabling processors` {

        @Test
        fun `disabling used-versions keeps unused versions in effective metadata`() = runTest {
            val result = Dependanger({
                                         versions {
                                             version("used", "1.0.0")
                                             version("unused", "2.0.0")
                                         }
                                         libraries {
                                             library("lib", "com.example:lib", versionRef("used"))
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                disableProcessor(ProcessorIds.USED_VERSIONS)
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.versions).containsKey("used")
            assertThat((result as DependangerResult.Success).effective.versions).containsKey("unused")
        }

        @Test
        fun `disabling compat-rules skips compatibility diagnostics`() = runTest {
            val result = Dependanger({
                                         libraries {
                                             library("lib-a", "com.example:lib-a:1.0.0")
                                             library("lib-b", "com.example:lib-b:1.0.0")
                                         }
                                         compatibility {
                                             mutualExclusion("conflict") {
                                                 libraries("lib-a", "lib-b")
                                                 severity = Severity.WARNING
                                                 message = "Conflicting libraries"
                                             }
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                disableProcessor(ProcessorIds.COMPAT_RULES)
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.diagnostics.warnings).noneMatch {
                it.message.contains("Conflicting libraries")
            }
        }

        @Test
        fun `disabling compat-rules skips compatibility rule evaluation`() = runTest {
            val result = Dependanger({
                                         libraries {
                                             library("lib", "com.example:lib:1.0.0")
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                disableProcessor(ProcessorIds.COMPAT_RULES)
            }.process()

            assertThat(result.isSuccess).isTrue()
            val compatDiagnostics = result.diagnostics.infos.filter {
                it.code == DiagnosticCodes.Compatibility.NO_CUSTOM_RULES
            }
            assertThat(compatDiagnostics).isEmpty()
        }

        @Test
        fun `disabling nonexistent processor ID does not fail pipeline`() = runTest {
            val result = Dependanger({
                                         libraries {
                                             library("lib", "com.example:lib:1.0.0")
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                disableProcessor("completely-nonexistent-processor")
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("lib")
        }

        @Test
        fun `disabling multiple processors simultaneously works`() = runTest {
            val result = Dependanger({
                                         versions {
                                             version("used", "1.0.0")
                                             version("unused", "2.0.0")
                                         }
                                         libraries {
                                             library("lib", "com.example:lib", versionRef("used"))
                                         }
                                         bundles {
                                             bundle("test-bundle") { libraries("lib", "missing-lib") }
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                disableProcessor(ProcessorIds.USED_VERSIONS)
                disableProcessor(ProcessorIds.VALIDATION_DUPLICATES)
                disableProcessor(ProcessorIds.VALIDATION_REFERENCES)
                disableProcessor(ProcessorIds.VALIDATION_CIRCULAR)
                disableProcessor(ProcessorIds.COMPAT_RULES)
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.versions).containsKey("unused")

            val validationDiagnostics = result.diagnostics.warnings.filter {
                it.code == DiagnosticCodes.Validation.BUNDLE_REF_MISSING
            } + result.diagnostics.errors.filter {
                it.code == DiagnosticCodes.Validation.BUNDLE_REF_MISSING
            }
            assertThat(validationDiagnostics).isEmpty()
        }
    }

    @Nested
    inner class `Custom processors` {

        private val testExtensionKey = ExtensionKey("test-key", String.serializer())

        @Test
        fun `custom processor receives effective metadata with resolved libraries`() = runTest {
            val receivedLibraries = mutableListOf<String>()

            val processor = FakeProcessor(
                id = "custom-lib-inspector",
                phase = ProcessingPhase.VALIDATION,
                constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)),
                extensionKey = testExtensionKey,
                provider = { metadata ->
                    receivedLibraries.addAll(metadata.libraries.keys)
                    "inspected"
                },
            )

            val result = Dependanger({
                                         versions { version("ktor", "3.1.1") }
                                         libraries {
                                             library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                                             library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(processor)
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(receivedLibraries).containsExactlyInAnyOrder("ktor-core", "ktor-cio")
        }

        @Test
        fun `custom processor extension data accessible via result effective extensions`() = runTest {
            val processor = FakeProcessor(
                id = "custom-enrichment",
                phase = ProcessingPhase.VALIDATION,
                constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)),
                extensionKey = testExtensionKey,
                provider = { metadata -> "found ${metadata.libraries.size} libraries" },
            )

            val result = Dependanger({
                                         libraries {
                                             library("lib-a", "com.a:lib:1.0.0")
                                             library("lib-b", "com.b:lib:2.0.0")
                                             library("lib-c", "com.c:lib:3.0.0")
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(processor)
            }.process()

            assertThat(result.isSuccess).isTrue()
            val extensionValue = (result as DependangerResult.Success).effective.extensions[testExtensionKey]
            assertThat(extensionValue).isEqualTo("found 3 libraries")
        }

        @Test
        fun `multiple custom processors at different phases all execute`() = runTest {
            val key1 = ExtensionKey("test-key-phase-transform", String.serializer())
            val key2 = ExtensionKey("test-key-phase-validation", String.serializer())

            val processor1 = FakeParallelProcessor(
                id = "custom-enrichment",
                phase = TEST_PHASE_A,
                constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)),
                extensionKey = key1,
                provider = { "transform-done" },
            )

            val processor2 = FakeParallelProcessor(
                id = "custom-validate",
                phase = TEST_PHASE_B,
                constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)),
                extensionKey = key2,
                provider = { "validation-done" },
            )

            val result = Dependanger({
                                         libraries { library("lib", "com.example:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(processor1)
                addProcessor(processor2)
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.extensions[key1]).isEqualTo("transform-done")
            assertThat((result as DependangerResult.Success).effective.extensions[key2]).isEqualTo("validation-done")
        }

        @Test
        fun `custom processor at high order number runs after core processors`() = runTest {
            val processor = FakeProcessor(
                id = "custom-late-runner",
                phase = ProcessingPhase.VALIDATION,
                constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.USED_VERSIONS)),
                extensionKey = testExtensionKey,
                provider = { "ran" },
            )

            val result = Dependanger({
                                         versions {
                                             version("used", "1.0.0")
                                             version("unused", "2.0.0")
                                         }
                                         libraries {
                                             library("lib", "com.example:lib", versionRef("used"))
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(processor)
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.extensions[testExtensionKey]).isEqualTo("ran")
            assertThat((result as DependangerResult.Success).effective.versions).containsKey("used")
            assertThat((result as DependangerResult.Success).effective.versions).doesNotContainKey("unused")
        }
    }

    @Nested
    inner class `Builder factory methods` {

        @Test
        fun `fromDsl creates working Dependanger`() = runTest {
            val result = Dependanger.fromDsl {
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                    library("assertj", "org.assertj:assertj-core:3.27.3")
                }
            }.build().process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(2)
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("kotlin-stdlib")
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("assertj")
        }

        @Test
        fun `fromMetadata with DSL-generated metadata produces same libraries as fromDsl`() = runTest {
            val dslBlock: DependangerDsl.() -> Unit = {
                versions { version("ktor", "3.1.1") }
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                    library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
                }
            }

            val dslResult = Dependanger.fromDsl(dslBlock).build().process()

            val metadata = DependangerDsl().apply(dslBlock).toMetadata()
            val metadataResult = Dependanger.fromMetadata(metadata).build().process()

            assertThat(dslResult.isSuccess).isTrue()
            assertThat(metadataResult.isSuccess).isTrue()
            assertThat((metadataResult as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrderElementsOf((dslResult as DependangerResult.Success).effective.libraries.keys)

            (dslResult as DependangerResult.Success).effective.libraries.forEach { (alias, dslLib) ->
                val metaLib = (metadataResult as DependangerResult.Success).effective.libraries[alias]!!
                assertThat(metaLib.coordinate).isEqualTo(dslLib.coordinate)
                assertThat(metaLib.version.valueOrNull).isEqualTo(dslLib.version.valueOrNull)
            }
        }

        @Test
        fun `fromJson round-trips correctly`() = runTest {
            val metadata = DependangerDsl().apply {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
                plugins {
                    plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin"))
                }
                bundles {
                    bundle("kotlin") { libraries("kotlin-stdlib") }
                }
            }.toMetadata()

            val format = JsonSerializationFormat()
            val json = format.serialize(metadata)
            val result = Dependanger.fromJson(json).build().process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("kotlin-stdlib")
            assertThat((result as DependangerResult.Success).effective.libraries["kotlin-stdlib"]!!.version.valueOrNull).isEqualTo("2.1.20")
            assertThat((result as DependangerResult.Success).effective.plugins).containsKey("kotlin-jvm")
            assertThat((result as DependangerResult.Success).effective.bundles).containsKey("kotlin")
        }

        @Test
        fun `builder without explicit preset uses DEFAULT`() = runTest {
            val dslBlock: DependangerDsl.() -> Unit = {
                versions {
                    version("used", "1.0.0")
                    version("unused", "2.0.0")
                }
                libraries {
                    library("lib", "com.example:lib", versionRef("used"))
                }
            }

            val explicitDefaultResult = Dependanger.fromDsl(dslBlock)
                .preset(ProcessingPreset.DEFAULT)
                .build()
                .process()

            val implicitDefaultResult = Dependanger.fromDsl(dslBlock)
                .build()
                .process()

            assertThat(explicitDefaultResult.isSuccess).isTrue()
            assertThat(implicitDefaultResult.isSuccess).isTrue()
            assertThat((implicitDefaultResult as DependangerResult.Success).effective.versions.keys)
                .containsExactlyInAnyOrderElementsOf((explicitDefaultResult as DependangerResult.Success).effective.versions.keys)
            assertThat((implicitDefaultResult as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrderElementsOf((explicitDefaultResult as DependangerResult.Success).effective.libraries.keys)
        }
    }

    @Nested
    inner class `Pipeline error resilience` {

        @Test
        fun `unresolved version ref produces error diagnostic but pipeline completes`() = runTest {
            val result = dependanger {
                libraries {
                    library("broken-lib", "com.example:broken", versionRef("nonexistent"))
                    library("good-lib", "com.example:good:1.0.0")
                }
            }.process()

            assertThat(result.diagnostics.errors).anyMatch {
                it.code == DiagnosticCodes.Version.UNRESOLVED
            }
            assertThat(result).isInstanceOf(DependangerResult.CompletedWithErrors::class.java)
            assertThat((result as DependangerResult.CompletedWithErrors).effective.libraries).containsKey("good-lib")
        }

        @Test
        fun `bundle referencing nonexistent library produces warning`() = runTest {
            val result = dependanger {
                libraries {
                    library("existing-lib", "com.example:existing:1.0.0")
                }
                bundles {
                    bundle("partial-bundle") { libraries("existing-lib", "nonexistent-lib") }
                }
            }.process()

            assertThat(result).isInstanceOf(DependangerResult.Success::class.java)

            val bundleDiagnostics = result.diagnostics.warnings.filter {
                it.code == DiagnosticCodes.Bundle.LIBRARY_MISSING ||
                        it.code == DiagnosticCodes.Validation.BUNDLE_REF_MISSING
            } + result.diagnostics.errors.filter {
                it.code == DiagnosticCodes.Bundle.LIBRARY_MISSING ||
                        it.code == DiagnosticCodes.Validation.BUNDLE_REF_MISSING
            }
            assertThat(bundleDiagnostics).isNotEmpty()
        }
    }
}
