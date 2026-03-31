package io.github.zenhelix.dependanger.integration.pipeline

import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.integration.support.assertResult
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Distribution Filtering E2E")
class DistributionFilteringE2ETest : IntegrationTestBase() {

    private fun catalogWithTaggedLibraries(): io.github.zenhelix.dependanger.core.dsl.DependangerDsl.() -> Unit = {
        versions {
            version("kotlin", "2.1.20")
            version("android-lib", "1.0.0")
            version("ios-lib", "2.0.0")
        }
        libraries {
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin")) {
                tags("common")
            }
            library("android-core", "com.android:core", versionRef("android-lib")) {
                tags("android")
            }
            library("ios-foundation", "io.ios:foundation", versionRef("ios-lib")) {
                tags("ios")
            }
        }
        distributions {
            distribution("android") {
                spec {
                    byTags {
                        include { anyOf("android", "common") }
                    }
                }
            }
            distribution("ios") {
                spec {
                    byTags {
                        include { anyOf("ios", "common") }
                    }
                }
            }
        }
    }

    @Nested
    inner class `by tags` {

        @Test
        fun `distribution by tags includes only matching libraries`() = runTest {
            val result = dependanger(dslBlock = catalogWithTaggedLibraries()).process(distribution = "android")

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            val libraries = success.effective.libraries
            assertThat(libraries.keys).contains("kotlin-stdlib", "android-core")
            assertThat(libraries.keys).doesNotContain("ios-foundation")
        }
    }

    @Nested
    inner class `by aliases` {

        @Test
        fun `distribution by aliases includes only named libraries`() = runTest {
            val result = dependanger {
                versions {
                    version("v1", "1.0.0")
                }
                libraries {
                    library("lib-a", "com.example:lib-a", versionRef("v1"))
                    library("lib-b", "com.example:lib-b", versionRef("v1"))
                    library("lib-c", "com.example:lib-c", versionRef("v1"))
                }
                distributions {
                    distribution("subset") {
                        spec {
                            byAliases {
                                include("lib-a", "lib-c")
                            }
                        }
                    }
                }
            }.process(distribution = "subset")

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            val libraries = success.effective.libraries
            assertThat(libraries.keys).containsExactlyInAnyOrder("lib-a", "lib-c")
        }
    }

    @Nested
    inner class `by groups` {

        @Test
        fun `distribution by groups includes matching group patterns`() = runTest {
            val result = dependanger {
                versions {
                    version("v1", "1.0.0")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("v1"))
                    library("guava", "com.google.guava:guava", versionRef("v1"))
                    library("slf4j", "org.slf4j:slf4j-api", versionRef("v1"))
                }
                distributions {
                    distribution("jetbrains-only") {
                        spec {
                            byGroups {
                                include { matching("org.jetbrains.*") }
                            }
                        }
                    }
                }
            }.process(distribution = "jetbrains-only")

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            val libraries = success.effective.libraries
            assertThat(libraries.keys).containsExactly("kotlin-stdlib")
        }
    }

    @Nested
    inner class `no distribution` {

        @Test
        fun `no distribution includes all libraries`() = runTest {
            val result = dependanger(dslBlock = catalogWithTaggedLibraries()).process()

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            val libraries = success.effective.libraries
            assertThat(libraries).hasSize(3)
        }
    }

    @Nested
    inner class `cascading` {

        @Test
        fun `distribution filtering cascades to bundles`() = runTest {
            val result = dependanger {
                versions {
                    version("v1", "1.0.0")
                }
                libraries {
                    library("lib-a", "com.example:lib-a", versionRef("v1")) {
                        tags("included")
                    }
                    library("lib-b", "com.example:lib-b", versionRef("v1")) {
                        tags("excluded")
                    }
                }
                bundles {
                    bundle("mixed") {
                        libraries("lib-a", "lib-b")
                    }
                }
                distributions {
                    distribution("filtered") {
                        spec {
                            byTags {
                                include { anyOf("included") }
                            }
                        }
                    }
                }
            }.process(distribution = "filtered")

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            assertThat(success.effective.libraries.keys).containsExactly("lib-a")

            val bundle = success.effective.bundles["mixed"]
            if (bundle != null) {
                assertThat(bundle.libraries).doesNotContain("lib-b")
            }
        }

        @Test
        fun `distribution filtering cascades to used versions`() = runTest {
            val result = dependanger {
                versions {
                    version("used-ver", "1.0.0")
                    version("unused-ver", "2.0.0")
                }
                libraries {
                    library("lib-a", "com.example:lib-a", versionRef("used-ver")) {
                        tags("included")
                    }
                    library("lib-b", "com.example:lib-b", versionRef("unused-ver")) {
                        tags("excluded")
                    }
                }
                distributions {
                    distribution("filtered") {
                        spec {
                            byTags {
                                include { anyOf("included") }
                            }
                        }
                    }
                }
            }.process(distribution = "filtered")

            assertResult(result).isSuccessful()

            val success = result as DependangerResult.Success
            val versions = success.effective.versions
            assertThat(versions.keys).contains("used-ver")
            assertThat(versions.keys).doesNotContain("unused-ver")
        }
    }

    @Nested
    inner class `multiple distributions` {

        @Test
        fun `multiple distributions produce different catalogs`() = runTest {
            val dsl = catalogWithTaggedLibraries()

            val androidResult = dependanger(dslBlock = dsl).process(distribution = "android")
            val iosResult = dependanger(dslBlock = dsl).process(distribution = "ios")

            assertResult(androidResult).isSuccessful()
            assertResult(iosResult).isSuccessful()

            val androidLibs = (androidResult as DependangerResult.Success).effective.libraries.keys
            val iosLibs = (iosResult as DependangerResult.Success).effective.libraries.keys

            assertThat(androidLibs).contains("android-core")
            assertThat(androidLibs).doesNotContain("ios-foundation")
            assertThat(iosLibs).contains("ios-foundation")
            assertThat(iosLibs).doesNotContain("android-core")

            // Both should include common
            assertThat(androidLibs).contains("kotlin-stdlib")
            assertThat(iosLibs).contains("kotlin-stdlib")
        }
    }

    @Nested
    inner class `preview filter` {

        @Test
        fun `preview filter shows included and excluded`() = runTest {
            val dep = dependanger(dslBlock = catalogWithTaggedLibraries())

            val preview = dep.previewFilter("android")

            assertThat(preview.distribution).isEqualTo("android")
            assertThat(preview.included.libraries.keys).contains("kotlin-stdlib", "android-core")
            assertThat(preview.excluded.libraries.keys).contains("ios-foundation")
        }
    }

    @Nested
    inner class `error cases` {

        @Test
        fun `nonexistent distribution produces error`() = runTest {
            val result = dependanger(dslBlock = catalogWithTaggedLibraries()).process(distribution = "nonexistent")

            val diagnostics = result.diagnostics
            val allCodes = (diagnostics.errors + diagnostics.warnings).map { it.code }
            assertThat(allCodes).contains(DiagnosticCodes.Profile.NOT_FOUND)
        }
    }
}
