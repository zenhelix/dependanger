package io.github.zenhelix.dependanger.integration.features

import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.api.vulnerabilities
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import io.github.zenhelix.dependanger.features.security.securityCheck
import io.github.zenhelix.dependanger.features.updates.updateCheck
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.integration.support.OsvVulnResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SecurityCheckE2ETest : IntegrationTestBase() {

    @Nested
    inner class VulnerabilityDetection {

        @Test
        fun `detects vulnerabilities from OSV API`() = runTest {
            mockHttp {
                osv {
                    vulnerabilities(
                        "com.example:vuln-lib", listOf(
                            OsvVulnResponse(
                                id = "GHSA-abc-123",
                                summary = "Remote code execution",
                                cvssScore = 9.8,
                                fixedVersion = "2.0.0"
                            )
                        )
                    )
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("vuln-lib", "com.example:vuln-lib:1.0.0")
                }
                settings {
                    securityCheck { enabled = true; cacheDirectory = cacheDirFor("security") }
                }
            }.process()

            assertThat(result.effective).isNotNull
            assertThat(result.vulnerabilities).isNotEmpty()
            assertThat(result.vulnerabilities).anyMatch { it.id == "GHSA-abc-123" }
            assertThat(result.vulnerabilities.first().summary).isEqualTo("Remote code execution")
        }

        @Test
        fun `no vulnerabilities for clean libraries`() = runTest {
            mockHttp {
                osv {
                    noVulnerabilities("com.safe:lib")
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("safe-lib", "com.safe:lib:1.0.0")
                }
                settings {
                    securityCheck { enabled = true; cacheDirectory = cacheDirFor("security") }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.vulnerabilities).isEmpty()
        }

        @Test
        fun `multiple vulnerabilities per library`() = runTest {
            mockHttp {
                osv {
                    vulnerabilities(
                        "com.example:multi-vuln", listOf(
                            OsvVulnResponse(
                                id = "GHSA-first",
                                summary = "SQL Injection",
                                cvssScore = 8.5,
                                fixedVersion = "1.1.0"
                            ),
                            OsvVulnResponse(
                                id = "GHSA-second",
                                summary = "XSS vulnerability",
                                cvssScore = 6.5,
                                fixedVersion = "1.2.0"
                            )
                        )
                    )
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("multi-vuln", "com.example:multi-vuln:1.0.0")
                }
                settings {
                    securityCheck { enabled = true; cacheDirectory = cacheDirFor("security") }
                }
            }.process()

            assertThat(result.effective).isNotNull
            assertThat(result.vulnerabilities).hasSize(2)
            assertThat(result.vulnerabilities.map { it.id })
                .containsExactlyInAnyOrder("GHSA-first", "GHSA-second")
        }

        @Test
        fun `CVSS score maps to correct severity`() = runTest {
            mockHttp {
                osv {
                    vulnerabilities(
                        "com.critical:lib", listOf(
                            OsvVulnResponse(id = "CRITICAL-1", summary = "Critical", cvssScore = 9.8)
                        )
                    )
                    vulnerabilities(
                        "com.high:lib", listOf(
                            OsvVulnResponse(id = "HIGH-1", summary = "High", cvssScore = 7.5)
                        )
                    )
                    vulnerabilities(
                        "com.medium:lib", listOf(
                            OsvVulnResponse(id = "MEDIUM-1", summary = "Medium", cvssScore = 4.0)
                        )
                    )
                    vulnerabilities(
                        "com.low:lib", listOf(
                            OsvVulnResponse(id = "LOW-1", summary = "Low", cvssScore = 1.0)
                        )
                    )
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("critical-lib", "com.critical:lib:1.0.0")
                    library("high-lib", "com.high:lib:1.0.0")
                    library("medium-lib", "com.medium:lib:1.0.0")
                    library("low-lib", "com.low:lib:1.0.0")
                }
                settings {
                    securityCheck { enabled = true; cacheDirectory = cacheDirFor("security") }
                }
            }.process()

            assertThat(result.effective).isNotNull
            val byId = result.vulnerabilities.associateBy { it.id }
            assertThat(byId["CRITICAL-1"]!!.severity).isEqualTo(VulnerabilitySeverity.CRITICAL)
            assertThat(byId["HIGH-1"]!!.severity).isEqualTo(VulnerabilitySeverity.HIGH)
            assertThat(byId["MEDIUM-1"]!!.severity).isEqualTo(VulnerabilitySeverity.MEDIUM)
            assertThat(byId["LOW-1"]!!.severity).isEqualTo(VulnerabilitySeverity.LOW)
        }

        @Test
        fun `fixed version is extracted from response`() = runTest {
            mockHttp {
                osv {
                    vulnerabilities(
                        "com.example:fixable", listOf(
                            OsvVulnResponse(
                                id = "GHSA-fix-me",
                                summary = "Fixable vulnerability",
                                cvssScore = 7.0,
                                fixedVersion = "1.5.0"
                            )
                        )
                    )
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("fixable", "com.example:fixable:1.0.0")
                }
                settings {
                    securityCheck { enabled = true; cacheDirectory = cacheDirFor("security") }
                }
            }.process()

            assertThat(result.effective).isNotNull
            assertThat(result.vulnerabilities).hasSize(1)
            assertThat(result.vulnerabilities.first().fixedVersion).isEqualTo("1.5.0")
        }
    }

    @Nested
    inner class SecurityWithOtherFeatures {

        @Test
        fun `vulnerabilities coexist with updates`() = runTest {
            mockHttp {
                maven {
                    metadata("com.example", "lib", listOf("1.0.0", "2.0.0"))
                }
                osv {
                    vulnerabilities(
                        "com.example:lib", listOf(
                            OsvVulnResponse(
                                id = "GHSA-coexist",
                                summary = "Vulnerability in lib",
                                cvssScore = 8.0,
                                fixedVersion = "2.0.0"
                            )
                        )
                    )
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
                }
            }.process()

            assertThat(result.effective).isNotNull
            assertThat(result.updates).isNotEmpty()
            assertThat(result.vulnerabilities).isNotEmpty()
            assertThat(result.updates.first().latestVersion).isEqualTo("2.0.0")
            assertThat(result.vulnerabilities.first().id).isEqualTo("GHSA-coexist")
        }
    }
}
