package io.github.zenhelix.dependanger.integration.features

import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.features.updates.updateCheck
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UpdateCheckE2ETest : IntegrationTestBase() {

    @Nested
    inner class BasicUpdateCheck {

        @Test
        fun `detects available updates for libraries`() = runTest {
            mockHttp {
                maven {
                    metadata("org.jetbrains.kotlin", "kotlin-stdlib", listOf("2.1.20", "2.2.0"))
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
                settings {
                    updateCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("versions")
                        repositories = listOf(
                            MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).isNotEmpty()
            assertThat(result.updates).anyMatch { it.alias == "kotlin-stdlib" }
            assertThat(result.updates.first { it.alias == "kotlin-stdlib" }.latestVersion).isEqualTo("2.2.0")
        }

        @Test
        fun `no updates when all libraries are current`() = runTest {
            mockHttp {
                maven {
                    metadata("org.jetbrains.kotlin", "kotlin-stdlib", listOf("2.1.20"))
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
                settings {
                    updateCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("versions")
                        repositories = listOf(
                            MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).isEmpty()
        }

        @Test
        fun `classifies updates as MAJOR, MINOR, PATCH`() = runTest {
            mockHttp {
                maven {
                    metadata("com.patch", "lib", listOf("1.0.0", "1.0.1"))
                    metadata("com.minor", "lib", listOf("1.0.0", "1.1.0"))
                    metadata("com.major", "lib", listOf("1.0.0", "2.0.0"))
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("patch-lib", "com.patch:lib:1.0.0")
                    library("minor-lib", "com.minor:lib:1.0.0")
                    library("major-lib", "com.major:lib:1.0.0")
                }
                settings {
                    updateCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("versions")
                        repositories = listOf(
                            MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).hasSize(3)

            val byAlias = result.updates.associateBy { it.alias }
            assertThat(byAlias["patch-lib"]!!.updateType).isEqualTo(UpdateType.PATCH)
            assertThat(byAlias["minor-lib"]!!.updateType).isEqualTo(UpdateType.MINOR)
            assertThat(byAlias["major-lib"]!!.updateType).isEqualTo(UpdateType.MAJOR)
        }

        @Test
        fun `library with ignoreUpdates is skipped`() = runTest {
            mockHttp {
                maven {
                    metadata("org.jetbrains.kotlin", "kotlin-stdlib", listOf("2.1.20", "2.2.0"))
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin")) {
                        ignoreUpdates = true
                    }
                }
                settings {
                    updateCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("versions")
                        repositories = listOf(
                            MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).isEmpty()
        }
    }

    @Nested
    inner class FilteringInteraction {

        @Test
        fun `distribution filtering happens before update check`() = runTest {
            mockHttp {
                maven {
                    metadata("com.android", "lib", listOf("1.0", "2.0"))
                    metadata("com.server", "lib", listOf("2.0", "3.0"))
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("android-lib", "com.android:lib:1.0") { tags("android") }
                    library("server-lib", "com.server:lib:2.0") { tags("server") }
                }
                distributions {
                    distribution("android") {
                        spec { byTags { include { anyOf("android") } } }
                    }
                }
                settings {
                    updateCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("versions")
                        repositories = listOf(
                            MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                    }
                }
            }.process(distribution = "android")

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).allMatch { it.alias == "android-lib" }
            assertThat(result.updates).noneMatch { it.alias == "server-lib" }
        }

        @Test
        fun `exclude patterns skip matching libraries`() = runTest {
            mockHttp {
                maven {
                    metadata("org.jetbrains.kotlin", "kotlin-stdlib", listOf("2.1.20", "2.2.0"))
                    metadata("com.example", "lib", listOf("1.0.0", "2.0.0"))
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                    library("example-lib", "com.example:lib:1.0.0")
                }
                settings {
                    updateCheck {
                        enabled = true
                        repositories = listOf(
                            MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                        excludePatterns = listOf("org.jetbrains.kotlin:*")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).noneMatch { it.group == "org.jetbrains.kotlin" }
            assertThat(result.updates).anyMatch { it.alias == "example-lib" }
        }
    }
}
