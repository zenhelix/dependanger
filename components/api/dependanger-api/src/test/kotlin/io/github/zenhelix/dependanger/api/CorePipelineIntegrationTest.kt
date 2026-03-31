package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CorePipelineIntegrationTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        jdk: Int? = null,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) {
        preset(preset)
        jdk?.let { jdkVersion(it) }
    }

    @Nested
    inner class VersionResolution {

        @Test
        fun `versions declared in DSL are resolved in effective metadata`() = runTest {
            val result = dependanger {
                versions {
                    version("kotlin", "2.1.20")
                    version("ktor", "3.1.1")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.versions).containsKey("kotlin")
            assertThat((result as DependangerResult.Success).effective.versions["kotlin"]!!.value).isEqualTo("2.1.20")
            assertThat((result as DependangerResult.Success).effective.versions["kotlin"]!!.source).isEqualTo(VersionSource.DECLARED)
        }

        @Test
        fun `library with version reference resolves to declared version`() = runTest {
            val result = dependanger {
                versions { version("ktor", "3.1.1") }
                libraries { library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = (result as DependangerResult.Success).effective.libraries["ktor-core"]!!
            assertThat(lib.version!!.value).isEqualTo("3.1.1")
        }

        @Test
        fun `unresolved version reference produces error diagnostic`() = runTest {
            val result = dependanger {
                libraries { library("some-lib", "com.example:some-lib", versionRef("nonexistent")) }
            }.process()

            assertThat(result.isSuccess).isFalse()
            assertThat(result.diagnostics.errors).anyMatch { it.code == DiagnosticCodes.Version.UNRESOLVED }
        }

        @Test
        fun `extracted versions are created from library literals`() = runTest {
            val result = dependanger {
                libraries { library("assertj", "org.assertj:assertj-core:3.27.3") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = (result as DependangerResult.Success).effective.libraries["assertj"]!!
            assertThat(lib.version!!.value).isEqualTo("3.27.3")
        }

        @Test
        fun `version fallback applies when JDK condition matches`() = runTest {
            val result = dependanger(jdk = 8) {
                versions {
                    version("guava", "33.0-jre") {
                        fallback("33.0-android") { jdkBelow(11) }
                    }
                }
                libraries { library("guava", "com.google.guava:guava", versionRef("guava")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries["guava"]!!.version!!.value).isEqualTo("33.0-android")
        }

        @Test
        fun `version fallback does not apply when condition does not match`() = runTest {
            val result = dependanger(jdk = 17) {
                versions {
                    version("guava", "33.0-jre") {
                        fallback("33.0-android") { jdkBelow(11) }
                    }
                }
                libraries { library("guava", "com.google.guava:guava", versionRef("guava")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries["guava"]!!.version!!.value).isEqualTo("33.0-jre")
        }
    }

    @Nested
    inner class LibraryProcessing {

        @Test
        fun `libraries from DSL appear in effective metadata`() = runTest {
            val result = dependanger {
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(2)
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("kotlin-stdlib")
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("ktor-core")
        }

        @Test
        fun `library tags are preserved in effective metadata`() = runTest {
            val result = dependanger {
                libraries {
                    library("assertj", "org.assertj:assertj-core:3.27.3") {
                        tags("test", "assertion")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries["assertj"]!!.tags)
                .containsExactlyInAnyOrder("test", "assertion")
        }

        @Test
        fun `deprecated library is marked in effective metadata`() = runTest {
            val result = dependanger {
                libraries {
                    library("old-lib", "com.example:old-lib:1.0.0") {
                        deprecated(replacedBy = "new-lib", message = "Use new-lib instead")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = (result as DependangerResult.Success).effective.libraries["old-lib"]!!
            assertThat(lib.isDeprecated).isTrue()
            assertThat(lib.deprecation!!.replacedBy).isEqualTo("new-lib")
        }
    }

    @Nested
    inner class DistributionFiltering {

        @Test
        fun `distribution filters libraries by tags`() = runTest {
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
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("android-lib")
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("common-lib")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("server-lib")
        }

        @Test
        fun `processing without distribution includes all libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.a:lib:1.0") { tags("a") }
                    library("lib-b", "com.b:lib:2.0") { tags("b") }
                }
                distributions {
                    distribution("subset") {
                        spec { byTags { include { anyOf("a") } } }
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(2)
        }

        @Test
        fun `distribution filters libraries by alias`() = runTest {
            val result = dependanger {
                libraries {
                    library("keep-me", "com.keep:lib:1.0")
                    library("drop-me", "com.drop:lib:2.0")
                }
                distributions {
                    distribution("minimal") {
                        spec { byAliases { include("keep-me") } }
                    }
                }
            }.process(distribution = "minimal")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("keep-me")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("drop-me")
        }

        @Test
        fun `nonexistent distribution produces error diagnostic`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0") }
            }.process(distribution = "nonexistent")

            assertThat(result.diagnostics.errors).anyMatch { it.code == DiagnosticCodes.Profile.NOT_FOUND }
        }
    }

    @Nested
    inner class BundleProcessing {

        @Test
        fun `bundles group libraries by alias`() = runTest {
            val result = dependanger {
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                    library("ktor-cio", "io.ktor:ktor-client-cio:3.1.1")
                    library("ktor-json", "io.ktor:ktor-serialization-json:3.1.1")
                }
                bundles { bundle("ktor") { libraries("ktor-core", "ktor-cio", "ktor-json") } }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.bundles["ktor"]!!.libraries)
                .containsExactlyInAnyOrder("ktor-core", "ktor-cio", "ktor-json")
        }

        @Test
        fun `bundle extends inherits libraries from parent`() = runTest {
            val result = dependanger {
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                    library("ktor-cio", "io.ktor:ktor-client-cio:3.1.1")
                    library("ktor-auth", "io.ktor:ktor-client-auth:3.1.1")
                }
                bundles {
                    bundle("ktor-base") { libraries("ktor-core", "ktor-cio") }
                    bundle("ktor-full") {
                        libraries("ktor-auth")
                        extends("ktor-base")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.bundles["ktor-full"]!!.libraries)
                .containsExactlyInAnyOrder("ktor-core", "ktor-cio", "ktor-auth")
        }

        @Test
        fun `filtered library is removed from bundle`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.a:lib:1.0") { tags("keep") }
                    library("lib-b", "com.b:lib:1.0") { tags("drop") }
                }
                bundles { bundle("mixed") { libraries("lib-a", "lib-b") } }
                distributions {
                    distribution("filtered") {
                        spec { byTags { include { anyOf("keep") } } }
                    }
                }
            }.process(distribution = "filtered")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.bundles["mixed"]!!.libraries).containsExactly("lib-a")
        }
    }

    @Nested
    inner class PluginProcessing {

        @Test
        fun `plugins from DSL appear with resolved version`() = runTest {
            val result = dependanger {
                versions { version("kotlin-plugin", "2.1.20") }
                plugins { plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin-plugin")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val plugin = (result as DependangerResult.Success).effective.plugins["kotlin-jvm"]!!
            assertThat(plugin.id).isEqualTo("org.jetbrains.kotlin.jvm")
            assertThat(plugin.version!!.value).isEqualTo("2.1.20")
        }
    }

    @Nested
    inner class Validation {

        @Test
        fun `validate returns success for valid DSL`() = runTest {
            val result = dependanger {
                libraries { library("lib", "org.example:valid:1.0.0") }
            }.validate()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.diagnostics.hasErrors).isFalse()
        }

        @Test
        fun `validate catches unresolved version references`() = runTest {
            val result = dependanger {
                libraries { library("broken-lib", "com.example:broken", versionRef("does-not-exist")) }
            }.validate()

            assertThat(result.diagnostics.errors).anyMatch { it.code == DiagnosticCodes.Version.UNRESOLVED }
        }
    }

    @Nested
    inner class BuilderConfiguration {

        @Test
        fun `fromDsl creates builder that produces valid result`() = runTest {
            val result = Dependanger.fromDsl {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.build().process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("lib")
        }

        @Test
        fun `fromJson round-trips through serialization`() = runTest {
            val metadata = DependangerDsl().apply {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.toMetadata()

            val json = JsonSerializationFormat().serialize(metadata)
            val result = Dependanger.fromJson(json).build().process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("lib")
        }

        @Test
        fun `invalid JSON throws DependangerConfigurationException`() {
            assertThatThrownBy { Dependanger.fromJson("not a json") }
                .isInstanceOf(DependangerConfigurationException::class.java)
                .hasMessageContaining("Failed to parse metadata from JSON")
        }

        @Test
        fun `empty DSL produces valid empty result`() = runTest {
            val result = dependanger {}.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).isEmpty()
            assertThat((result as DependangerResult.Success).effective.plugins).isEmpty()
            assertThat((result as DependangerResult.Success).effective.bundles).isEmpty()
        }
    }

    @Nested
    inner class PreviewFilter {

        @Test
        fun `previewFilter shows included and excluded items`() = runTest {
            val d = dependanger {
                libraries {
                    library("android-lib", "com.android:lib:1.0") { tags("android") }
                    library("server-lib", "com.server:lib:2.0") { tags("server") }
                }
                distributions {
                    distribution("android") {
                        spec { byTags { include { anyOf("android") } } }
                    }
                }
            }

            val preview = d.previewFilter("android")

            assertThat(preview.distribution).isEqualTo("android")
            assertThat(preview.included.libraries).containsKey("android-lib")
            assertThat(preview.excluded.libraries).containsKey("server-lib")
        }
    }

    @Nested
    inner class ArtifactGeneration {

        @Test
        fun `process then toToml produces valid TOML`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries { library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin")) }
                plugins { plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin")) }
                bundles { bundle("kotlin") { libraries("kotlin-stdlib") } }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("[versions]")
            assertThat(toml).contains("[libraries]")
            assertThat(toml).contains("[plugins]")
            assertThat(toml).contains("[bundles]")
        }

        @Test
        fun `process then toBom produces valid BOM XML`() = runTest {
            val result = dependanger {
                libraries { library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(
                BomConfig(
                    groupId = "io.github.zenhelix",
                    artifactId = "test-bom",
                    version = "1.0.0",
                    name = null,
                    description = null,
                    filename = "bom.pom.xml",
                    includeOptionalDependencies = false,
                    prettyPrint = false,
                    includeDeprecationComments = false,
                )
            )
            assertThat(bom).contains("<groupId>io.github.zenhelix</groupId>")
            assertThat(bom).contains("kotlin-stdlib")
        }
    }

    @Nested
    inner class ResultExtensions {

        @Test
        fun `extension properties return empty lists on default preset`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
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
        fun `extension properties are safe on failed result`() {
            val result = DependangerResult.Failure(diagnostics = Diagnostics.EMPTY)

            assertThat(result.updates).isEmpty()
            assertThat(result.vulnerabilities).isEmpty()
            assertThat(result.licenseViolations).isEmpty()
            assertThat(result.transitives).isEmpty()
            assertThat(result.compatibilityIssues).isEmpty()
            assertThat(result.versionConflicts).isEmpty()
        }
    }

    @Nested
    inner class UsedVersionsCleanup {

        @Test
        fun `unused versions are removed from effective metadata`() = runTest {
            val result = dependanger {
                versions {
                    version("used", "1.0.0")
                    version("unused", "2.0.0")
                }
                libraries { library("lib", "com.example:lib", versionRef("used")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.versions).containsKey("used")
            assertThat((result as DependangerResult.Success).effective.versions).doesNotContainKey("unused")
        }
    }
}
