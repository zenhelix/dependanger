package io.github.zenhelix.dependanger.integration.features

import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.api.licenseViolations
import io.github.zenhelix.dependanger.api.toBom
import io.github.zenhelix.dependanger.api.toToml
import io.github.zenhelix.dependanger.api.transitives
import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.api.vulnerabilities
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.settings.license.licenseCheck
import io.github.zenhelix.dependanger.feature.model.settings.security.securityCheck
import io.github.zenhelix.dependanger.feature.model.settings.transitive.transitiveResolution
import io.github.zenhelix.dependanger.feature.model.settings.updates.updateCheck
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.integration.support.MavenResponses
import io.github.zenhelix.dependanger.integration.support.OsvVulnResponse
import io.github.zenhelix.dependanger.integration.support.PomDep
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CombinedFeaturesE2ETest : IntegrationTestBase() {

    @Test
    fun `STRICT preset runs all features and produces complete result`() = runTest {
        mockHttp {
            maven {
                metadata("org.jetbrains.kotlin", "kotlin-stdlib", listOf("2.1.20", "2.2.0"))
                pom(
                    "org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20",
                    MavenResponses.pomXml(
                        "org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20",
                        dependencies = listOf(PomDep("org.jetbrains", "annotations", "24.0.0"))
                    )
                )
                metadata("org.jetbrains", "annotations", listOf("24.0.0"))
                pom(
                    "org.jetbrains", "annotations", "24.0.0",
                    MavenResponses.pomXml("org.jetbrains", "annotations", "24.0.0")
                )
            }
            osv {
                noVulnerabilities("org.jetbrains.kotlin:kotlin-stdlib")
            }
            clearlyDefined {
                license("org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20", "Apache-2.0")
            }
        }

        val result = dependanger(ProcessingPreset.STRICT) {
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
            settings {
                updateCheck {
                    enabled = true
                    cacheDirectory = cacheDirFor("versions")
                    repositories = listOf(
                        MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                    )
                }
                securityCheck { enabled = true; cacheDirectory = cacheDirFor("security") }
                licenseCheck {
                    enabled = true
                    cacheDirectory = cacheDirFor("licenses")
                    allowedLicenses = listOf("Apache-2.0")
                }
                transitiveResolution {
                    enabled = true
                    cacheDirectory = cacheDirFor("transitives")
                    repositories = listOf(
                        MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                    )
                }
            }
        }.process()

        assertThat(result).isInstanceOf(DependangerResult.Success::class.java)
        val success = result as DependangerResult.Success
        assertThat(success.effective.libraries).containsKey("kotlin-stdlib")
        assertThat(success.effective.plugins).containsKey("kotlin-jvm")
        assertThat(success.effective.bundles).containsKey("kotlin")

        // Updates available
        assertThat(result.updates).isNotEmpty()
        assertThat(result.updates.first().latestVersion).isEqualTo("2.2.0")

        // No vulnerabilities for clean lib
        assertThat(result.vulnerabilities).isEmpty()

        // No license violations for allowed license
        assertThat(result.licenseViolations).isEmpty()

        // Transitives resolved
        assertThat(result.transitives).isNotEmpty()
    }

    @Test
    fun `all feature data flows through to TOML generation`() = runTest {
        mockHttp {
            maven {
                metadata("com.example", "lib", listOf("1.0.0", "2.0.0"))
                pom(
                    "com.example", "lib", "1.0.0",
                    MavenResponses.pomXml("com.example", "lib", "1.0.0")
                )
            }
            osv {
                noVulnerabilities("com.example:lib")
            }
            clearlyDefined {
                license("com.example", "lib", "1.0.0", "MIT")
            }
        }

        val result = dependanger(ProcessingPreset.STRICT) {
            libraries {
                library("lib", "com.example:lib:1.0.0")
            }
            settings {
                updateCheck {
                    enabled = true
                    cacheDirectory = cacheDirFor("versions")
                    repositories = listOf(
                        MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                    )
                }
                securityCheck { enabled = true; cacheDirectory = cacheDirFor("security") }
                licenseCheck {
                    enabled = true
                    cacheDirectory = cacheDirFor("licenses")
                    allowedLicenses = listOf("MIT")
                }
            }
        }.process()

        assertThat(result.isSuccess).isTrue()

        val toml = result.toToml()
        assertThat(toml).contains("[libraries]")
        assertThat(toml).contains("com.example")
        assertThat(toml).contains("lib")
    }

    @Test
    fun `all feature data flows through to BOM generation`() = runTest {
        mockHttp {
            maven {
                metadata("com.example", "lib", listOf("1.0.0", "2.0.0"))
                pom(
                    "com.example", "lib", "1.0.0",
                    MavenResponses.pomXml("com.example", "lib", "1.0.0")
                )
            }
            osv {
                noVulnerabilities("com.example:lib")
            }
            clearlyDefined {
                license("com.example", "lib", "1.0.0", "Apache-2.0")
            }
        }

        val result = dependanger(ProcessingPreset.STRICT) {
            libraries {
                library("lib", "com.example:lib:1.0.0")
            }
            settings {
                updateCheck {
                    enabled = true
                    cacheDirectory = cacheDirFor("versions")
                    repositories = listOf(
                        MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
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

        assertThat(result.isSuccess).isTrue()

        val bom = result.toBom(bomConfig(groupId = "io.github.zenhelix"))
        assertThat(bom).contains("<groupId>com.example</groupId>")
        assertThat(bom).contains("<artifactId>lib</artifactId>")
        assertThat(bom).contains("<version>1.0.0</version>")
    }

    @Test
    fun `feature results survive parallel execution`() = runTest {
        mockHttp {
            maven {
                metadata("com.example", "lib-a", listOf("1.0.0", "2.0.0"))
                metadata("com.example", "lib-b", listOf("1.0.0", "3.0.0"))
            }
            osv {
                vulnerabilities(
                    "com.example:lib-a", listOf(
                        OsvVulnResponse(id = "GHSA-parallel-a", summary = "Vuln A", cvssScore = 7.0)
                    )
                )
                noVulnerabilities("com.example:lib-b")
            }
        }

        val result = dependanger(ProcessingPreset.STRICT) {
            libraries {
                library("lib-a", "com.example:lib-a:1.0.0")
                library("lib-b", "com.example:lib-b:1.0.0")
            }
            settings {
                updateCheck {
                    enabled = true
                    cacheDirectory = cacheDirFor("versions")
                    repositories = listOf(
                        MavenRepository(url = router.mavenBaseUrl, name = "Mock Maven")
                    )
                }
                securityCheck { enabled = true; cacheDirectory = cacheDirFor("security") }
            }
        }.process()

        assertThat(result).isInstanceOf(DependangerResult.CompletedWithErrors::class.java)

        // UPDATE_CHECK (PARALLEL_IO) produced data
        assertThat(result.updates).isNotEmpty()
        assertThat(result.updates.map { it.alias }).containsExactlyInAnyOrder("lib-a", "lib-b")

        // SECURITY_CHECK (PARALLEL_IO) produced data
        assertThat(result.vulnerabilities).isNotEmpty()
        assertThat(result.vulnerabilities.first().id).isEqualTo("GHSA-parallel-a")
    }
}
