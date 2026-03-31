package io.github.zenhelix.dependanger.integration.features

import io.github.zenhelix.dependanger.api.transitives
import io.github.zenhelix.dependanger.api.versionConflicts
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.settings.transitive.transitiveResolution
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.integration.support.MavenResponses
import io.github.zenhelix.dependanger.integration.support.PomDep
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TransitiveResolutionE2ETest : IntegrationTestBase() {

    @Nested
    inner class BasicResolution {

        @Test
        fun `resolves direct dependencies from POM`() = runTest {
            mockHttp {
                maven {
                    metadata("com.example", "parent-lib", listOf("1.0.0"))
                    pom(
                        "com.example", "parent-lib", "1.0.0",
                        MavenResponses.pomXml(
                            "com.example", "parent-lib", "1.0.0",
                            dependencies = listOf(
                                PomDep("com.example", "child-a", "1.0.0"),
                                PomDep("com.example", "child-b", "2.0.0")
                            )
                        )
                    )
                    metadata("com.example", "child-a", listOf("1.0.0"))
                    pom(
                        "com.example", "child-a", "1.0.0",
                        MavenResponses.pomXml("com.example", "child-a", "1.0.0")
                    )
                    metadata("com.example", "child-b", listOf("2.0.0"))
                    pom(
                        "com.example", "child-b", "2.0.0",
                        MavenResponses.pomXml("com.example", "child-b", "2.0.0")
                    )
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("parent-lib", "com.example:parent-lib:1.0.0")
                }
                settings {
                    transitiveResolution {
                        enabled = true
                        cacheDirectory = cacheDirFor("transitives")
                        repositories = listOf(
                            MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.transitives).isNotEmpty()

            val parentTree = result.transitives.find {
                it.group == "com.example" && it.artifact == "parent-lib"
            }
            assertThat(parentTree).isNotNull
            assertThat(parentTree!!.children).hasSize(2)
            assertThat(parentTree.children.map { it.artifact })
                .containsExactlyInAnyOrder("child-a", "child-b")
        }

        @Test
        fun `resolves transitive dependency chain`() = runTest {
            mockHttp {
                maven {
                    metadata("com.a", "lib-a", listOf("1.0.0"))
                    pom(
                        "com.a", "lib-a", "1.0.0",
                        MavenResponses.pomXml(
                            "com.a", "lib-a", "1.0.0",
                            dependencies = listOf(PomDep("com.b", "lib-b", "1.0.0"))
                        )
                    )
                    metadata("com.b", "lib-b", listOf("1.0.0"))
                    pom(
                        "com.b", "lib-b", "1.0.0",
                        MavenResponses.pomXml(
                            "com.b", "lib-b", "1.0.0",
                            dependencies = listOf(PomDep("com.c", "lib-c", "1.0.0"))
                        )
                    )
                    metadata("com.c", "lib-c", listOf("1.0.0"))
                    pom(
                        "com.c", "lib-c", "1.0.0",
                        MavenResponses.pomXml("com.c", "lib-c", "1.0.0")
                    )
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("lib-a", "com.a:lib-a:1.0.0")
                }
                settings {
                    transitiveResolution {
                        enabled = true
                        cacheDirectory = cacheDirFor("transitives")
                        repositories = listOf(
                            MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.transitives).isNotEmpty()

            val treeA = result.transitives.find {
                it.group == "com.a" && it.artifact == "lib-a"
            }
            assertThat(treeA).isNotNull
            assertThat(treeA!!.children).hasSize(1)

            val treeB = treeA.children.first()
            assertThat(treeB.artifact).isEqualTo("lib-b")
            assertThat(treeB.children).hasSize(1)
            assertThat(treeB.children.first().artifact).isEqualTo("lib-c")
        }

        @Test
        fun `detects version conflicts`() = runTest {
            mockHttp {
                maven {
                    metadata("com.a", "lib-a", listOf("1.0.0"))
                    pom(
                        "com.a", "lib-a", "1.0.0",
                        MavenResponses.pomXml(
                            "com.a", "lib-a", "1.0.0",
                            dependencies = listOf(PomDep("com.shared", "common", "1.0.0"))
                        )
                    )
                    metadata("com.b", "lib-b", listOf("1.0.0"))
                    pom(
                        "com.b", "lib-b", "1.0.0",
                        MavenResponses.pomXml(
                            "com.b", "lib-b", "1.0.0",
                            dependencies = listOf(PomDep("com.shared", "common", "2.0.0"))
                        )
                    )
                    metadata("com.shared", "common", listOf("1.0.0", "2.0.0"))
                    pom(
                        "com.shared", "common", "1.0.0",
                        MavenResponses.pomXml("com.shared", "common", "1.0.0")
                    )
                    pom(
                        "com.shared", "common", "2.0.0",
                        MavenResponses.pomXml("com.shared", "common", "2.0.0")
                    )
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("lib-a", "com.a:lib-a:1.0.0")
                    library("lib-b", "com.b:lib-b:1.0.0")
                }
                settings {
                    transitiveResolution {
                        enabled = true
                        cacheDirectory = cacheDirFor("transitives")
                        repositories = listOf(
                            MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                        )
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.versionConflicts).isNotEmpty()

            val conflict = result.versionConflicts.find {
                it.group == "com.shared" && it.artifact == "common"
            }
            assertThat(conflict).isNotNull
            assertThat(conflict!!.requestedVersions).containsExactlyInAnyOrder("1.0.0", "2.0.0")
        }
    }
}
