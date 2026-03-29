package io.github.zenhelix.dependanger.integration.pipeline

import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.integration.support.assertResult
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Version Resolution Chain E2E")
class VersionResolutionChainE2ETest : IntegrationTestBase() {

    @Nested
    inner class `literal versions` {

        @Test
        fun `literal version in library coordinates`() = runTest {
            val result = dependanger {
                libraries {
                    library("guava", "com.google.guava:guava:33.0.0")
                }
            }.process()

            assertResult(result).isSuccessful()

            val lib = result.effective!!.libraries["guava"]
            assertThat(lib).isNotNull
            assertThat(lib!!.version).isNotNull
            assertThat(lib.version!!.value).isEqualTo("33.0.0")
        }
    }

    @Nested
    inner class `version references` {

        @Test
        fun `version reference resolves to declared version`() = runTest {
            val result = dependanger {
                versions {
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
            }.process()

            assertResult(result).isSuccessful()

            val lib = result.effective!!.libraries["kotlin-stdlib"]
            assertThat(lib).isNotNull
            assertThat(lib!!.version).isNotNull
            assertThat(lib.version!!.value).isEqualTo("2.1.20")
        }
    }

    @Nested
    inner class `extracted versions` {

        @Test
        fun `extracted version creates shared version entry`() = runTest {
            val result = dependanger {
                libraries {
                    library("guava", "com.google.guava:guava:33.0.0")
                }
            }.process()

            assertResult(result).isSuccessful()

            val effective = result.effective!!
            // Literal versions may be extracted into the versions map
            val hasVersion = effective.versions.values.any { it.value == "33.0.0" }
            assertThat(hasVersion).isTrue()
        }
    }

    @Nested
    inner class `unresolved references` {

        @Test
        fun `unresolved version reference produces error diagnostic`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib", "com.example:lib", versionRef("nonexistent"))
                }
            }.process()

            val diagnostics = result.diagnostics
            val errorCodes = diagnostics.errors.map { it.code }
            assertThat(errorCodes).contains(DiagnosticCodes.Version.UNRESOLVED)
        }
    }

    @Nested
    inner class `version fallbacks` {

        @Test
        fun `version fallback applies when JDK condition matches`() = runTest {
            val result = dependanger(jdk = 8) {
                versions {
                    version("lib-ver", "2.0.0") {
                        fallback("1.0.0") { jdkBelow(11) }
                    }
                }
                libraries {
                    library("my-lib", "com.example:my-lib", versionRef("lib-ver"))
                }
            }.process()

            assertResult(result).isSuccessful()

            val version = result.effective!!.versions["lib-ver"]
            assertThat(version).isNotNull
            assertThat(version!!.value).isEqualTo("1.0.0")
        }

        @Test
        fun `version fallback does not apply when condition does not match`() = runTest {
            val result = dependanger(jdk = 17) {
                versions {
                    version("lib-ver", "2.0.0") {
                        fallback("1.0.0") { jdkBelow(11) }
                    }
                }
                libraries {
                    library("my-lib", "com.example:my-lib", versionRef("lib-ver"))
                }
            }.process()

            assertResult(result).isSuccessful()

            val version = result.effective!!.versions["lib-ver"]
            assertThat(version).isNotNull
            assertThat(version!!.value).isEqualTo("2.0.0")
        }

        @Test
        fun `version fallback with kotlin version condition`() = runTest {
            val result = dependanger {
                versions {
                    version("lib-ver", "3.0.0") {
                        fallback("2.5.0") { kotlinVersionBelow("2.0.0") }
                    }
                }
                libraries {
                    library("my-lib", "com.example:my-lib", versionRef("lib-ver"))
                }
            }.process()

            assertResult(result).isSuccessful()

            // Without kotlin version set, fallback condition should not match
            val version = result.effective!!.versions["lib-ver"]
            assertThat(version).isNotNull
            assertThat(version!!.value).isEqualTo("3.0.0")
        }

        @Test
        fun `multiple fallbacks evaluated in order`() = runTest {
            val result = dependanger(jdk = 8) {
                versions {
                    version("lib-ver", "3.0.0") {
                        fallback("1.0.0") { jdkBelow(9) }
                        fallback("2.0.0") { jdkBelow(17) }
                    }
                }
                libraries {
                    library("my-lib", "com.example:my-lib", versionRef("lib-ver"))
                }
            }.process()

            assertResult(result).isSuccessful()

            val version = result.effective!!.versions["lib-ver"]
            assertThat(version).isNotNull
            // JDK 8 < 9, so first fallback should win
            assertThat(version!!.value).isEqualTo("1.0.0")
        }
    }

    @Nested
    inner class `version pruning` {

        @Test
        fun `unused versions pruned from effective`() = runTest {
            val result = dependanger {
                versions {
                    version("used", "1.0.0")
                    version("unused-1", "2.0.0")
                    version("unused-2", "3.0.0")
                }
                libraries {
                    library("lib", "com.example:lib", versionRef("used"))
                }
            }.process()

            assertResult(result).isSuccessful()

            val versions = result.effective!!.versions
            assertThat(versions.keys).contains("used")
            assertThat(versions.keys).doesNotContain("unused-1", "unused-2")
        }

        @Test
        fun `version used by plugin is not pruned when plugin has matching tag`() = runTest {
            val result = dependanger {
                versions {
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin")) {
                        tags("server")
                    }
                }
                plugins {
                    plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin")) {
                        tags("client")
                    }
                }
                distributions {
                    distribution("no-kotlin-lib") {
                        spec {
                            byTags {
                                include { anyOf("client") }
                            }
                        }
                    }
                }
            }.process(distribution = "no-kotlin-lib")

            assertResult(result).isSuccessful()

            val effective = result.effective!!
            // Library should be filtered out (has "server" tag, not "client")
            assertThat(effective.libraries.keys).doesNotContain("kotlin-stdlib")
            // Plugin survives because it has "client" tag
            assertThat(effective.plugins.keys).contains("kotlin-jvm")
            // Version should survive because plugin uses it
            assertThat(effective.versions.keys).contains("kotlin")
        }
    }
}
