package io.github.zenhelix.dependanger.integration.features

import io.github.zenhelix.dependanger.api.licenseViolations
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.features.license.licenseCheck
import io.github.zenhelix.dependanger.features.license.model.LicenseViolationType
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LicenseCheckE2ETest : IntegrationTestBase() {

    @Nested
    inner class LicenseDetection {

        @Test
        fun `detects license from ClearlyDefined API`() = runTest {
            mockHttp {
                clearlyDefined {
                    license("org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20", "Apache-2.0")
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                }
                settings {
                    licenseCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("licenses")
                        allowedLicenses = listOf("Apache-2.0", "MIT")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).isEmpty()
        }

        @Test
        fun `license from Maven POM fallback`() = runTest {
            val pomWithLicense = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>pom-licensed</artifactId>
                  <version>1.0.0</version>
                  <licenses>
                    <license>
                      <name>MIT License</name>
                      <url>https://opensource.org/licenses/MIT</url>
                    </license>
                  </licenses>
                </project>
            """.trimIndent()

            mockHttp {
                clearlyDefined {
                    noLicense("com.example", "pom-licensed", "1.0.0")
                }
                maven {
                    pom("com.example", "pom-licensed", "1.0.0", pomWithLicense)
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("pom-licensed", "com.example:pom-licensed:1.0.0")
                }
                settings {
                    licenseCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("licenses")
                        allowedLicenses = listOf("MIT")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).isEmpty()
        }

        @Test
        fun `denied license produces violation`() = runTest {
            mockHttp {
                clearlyDefined {
                    license("com.gpl", "gpl-lib", "1.0.0", "GPL-3.0-only")
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("gpl-lib", "com.gpl:gpl-lib:1.0.0")
                }
                settings {
                    licenseCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("licenses")
                        allowedLicenses = listOf("Apache-2.0", "MIT")
                        deniedLicenses = listOf("GPL-3.0-only")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).isNotEmpty()
            assertThat(result.licenseViolations).anyMatch { violation ->
                violation.alias == "gpl-lib" &&
                        violation.detectedLicense == "GPL-3.0-only" &&
                        violation.violationType == LicenseViolationType.DENIED
            }
        }

        @Test
        fun `permissive license passes`() = runTest {
            mockHttp {
                clearlyDefined {
                    license("com.mit", "mit-lib", "1.0.0", "MIT")
                    license("com.apache", "apache-lib", "2.0.0", "Apache-2.0")
                }
            }

            val result = dependanger(ProcessingPreset.STRICT) {
                libraries {
                    library("mit-lib", "com.mit:mit-lib:1.0.0")
                    library("apache-lib", "com.apache:apache-lib:2.0.0")
                }
                settings {
                    licenseCheck {
                        enabled = true
                        cacheDirectory = cacheDirFor("licenses")
                        allowedLicenses = listOf("MIT", "Apache-2.0")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).isEmpty()
        }
    }
}
