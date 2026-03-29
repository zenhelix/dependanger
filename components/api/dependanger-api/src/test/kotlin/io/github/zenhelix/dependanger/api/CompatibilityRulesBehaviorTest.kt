package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.VersionConstraintType
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CompatibilityRulesBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        jdk: Int? = null,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) {
        preset(preset)
        jdk?.let { jdkVersion(it) }
    }

    @Nested
    inner class `JDK requirement rules` {

        @Test
        fun `jdk requirement violation produces error when jdk is below minimum`() = runTest {
            val result = dependanger(jdk = 11) {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                    library("spring-web", "org.springframework:spring-web:6.1.0")
                }
                compatibility {
                    jdkRequirement("spring6-jdk17") {
                        matches = "org.springframework*"
                        minJdk = 17
                        severity = Severity.ERROR
                        message = "Spring 6 requires JDK 17+"
                    }
                }
            }.process()

            assertThat(result.isSuccess).isFalse()
            assertThat(result.diagnostics.errors).anyMatch { it.message.contains("Spring 6 requires JDK 17+") }
        }

        @Test
        fun `jdk requirement passes when jdk meets minimum`() = runTest {
            val result = dependanger(jdk = 17) {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                    library("spring-web", "org.springframework:spring-web:6.1.0")
                }
                compatibility {
                    jdkRequirement("spring6-jdk17") {
                        matches = "org.springframework*"
                        minJdk = 17
                        severity = Severity.ERROR
                        message = "Spring 6 requires JDK 17+"
                    }
                }
            }.process()

            assertThat(result.diagnostics.errors).noneMatch { it.message.contains("Spring 6 requires JDK 17+") }
        }

        @Test
        fun `jdk requirement with maxJdk produces error when jdk exceeds maximum`() = runTest {
            val result = dependanger(jdk = 21) {
                libraries {
                    library("legacy-lib", "com.legacy:legacy-lib:1.0.0")
                }
                compatibility {
                    jdkRequirement("legacy-max-jdk") {
                        matches = "com.legacy*"
                        maxJdk = 17
                        severity = Severity.ERROR
                        message = "Legacy library does not support JDK above 17"
                    }
                }
            }.process()

            assertThat(result.isSuccess).isFalse()
            assertThat(result.diagnostics.errors).anyMatch {
                it.message.contains("Legacy library does not support JDK above 17")
            }
        }

        @Test
        fun `jdk requirement only checks libraries matching the pattern`() = runTest {
            val result = dependanger(jdk = 11) {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                }
                compatibility {
                    jdkRequirement("spring6-jdk17") {
                        matches = "org.springframework*"
                        minJdk = 17
                        severity = Severity.ERROR
                        message = "Spring 6 requires JDK 17+"
                    }
                }
            }.process()

            assertThat(result.diagnostics.errors).anyMatch { it.message.contains("Spring 6 requires JDK 17+") }
            assertThat(result.diagnostics.errors).noneMatch { it.message.contains("ktor") }
        }

        @Test
        fun `jdk requirement skipped when no jdk version provided`() = runTest {
            val result = dependanger(jdk = null) {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                }
                compatibility {
                    jdkRequirement("spring6-jdk17") {
                        matches = "org.springframework*"
                        minJdk = 17
                        severity = Severity.ERROR
                        message = "Spring 6 requires JDK 17+"
                    }
                }
            }.process()

            assertThat(result.diagnostics.errors).noneMatch { it.message.contains("Spring 6 requires JDK 17+") }
        }
    }

    @Nested
    inner class `Mutual exclusion rules` {

        @Test
        fun `mutual exclusion violation when conflicting libraries present`() = runTest {
            val result = dependanger {
                libraries {
                    library("log4j", "org.apache.logging.log4j:log4j-core:2.23.0")
                    library("logback", "ch.qos.logback:logback-classic:1.5.6")
                }
                compatibility {
                    mutualExclusion("logging-conflict") {
                        libraries("log4j", "logback")
                        severity = Severity.WARNING
                        message = "Use only one logging framework"
                    }
                }
            }.process()

            assertThat(result.diagnostics.warnings).anyMatch { it.message.contains("Use only one logging framework") }
        }

        @Test
        fun `mutual exclusion passes when only one library present`() = runTest {
            val result = dependanger {
                libraries {
                    library("log4j", "org.apache.logging.log4j:log4j-core:2.23.0")
                }
                compatibility {
                    mutualExclusion("logging-conflict") {
                        libraries("log4j", "logback")
                        severity = Severity.WARNING
                        message = "Use only one logging framework"
                    }
                }
            }.process()

            assertThat(result.diagnostics.warnings).noneMatch {
                it.message.contains("Use only one logging framework")
            }
        }

        @Test
        fun `mutual exclusion with distribution filtering removes conflict`() = runTest {
            val result = dependanger {
                libraries {
                    library("log4j", "org.apache.logging.log4j:log4j-core:2.23.0") { tags("logging-a") }
                    library("logback", "ch.qos.logback:logback-classic:1.5.6") { tags("logging-b") }
                }
                distributions {
                    distribution("only-log4j") {
                        spec { byTags { include { anyOf("logging-a") } } }
                    }
                }
                compatibility {
                    mutualExclusion("logging-conflict") {
                        libraries("log4j", "logback")
                        severity = Severity.WARNING
                        message = "Use only one logging framework"
                    }
                }
            }.process(distribution = "only-log4j")

            assertThat(result.diagnostics.warnings).noneMatch {
                it.message.contains("Use only one logging framework")
            }
        }

        @Test
        fun `mutual exclusion with error severity produces error diagnostic`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.a:lib-a:1.0.0")
                    library("lib-b", "com.b:lib-b:1.0.0")
                }
                compatibility {
                    mutualExclusion("strict-exclusion") {
                        libraries("lib-a", "lib-b")
                        severity = Severity.ERROR
                        message = "Libraries lib-a and lib-b are mutually exclusive"
                    }
                }
            }.process()

            assertThat(result.isSuccess).isFalse()
            assertThat(result.diagnostics.errors).anyMatch {
                it.message.contains("Libraries lib-a and lib-b are mutually exclusive")
            }
        }
    }

    @Nested
    inner class `Version constraint rules` {

        @Test
        fun `same version constraint violated when versions differ`() = runTest {
            val result = dependanger {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                    library("spring-web", "org.springframework:spring-web:6.0.0")
                }
                compatibility {
                    versionConstraint("spring-same-version") {
                        libraries("spring-core", "spring-web")
                        constraint = VersionConstraintType.SAME_VERSION
                        severity = Severity.ERROR
                    }
                }
            }.process()

            assertThat(result.isSuccess).isFalse()
            assertThat(result.diagnostics.errors).anyMatch {
                it.message.contains("SAME_VERSION") && it.message.contains("spring-core") && it.message.contains("spring-web")
            }
        }

        @Test
        fun `same version constraint passes when versions match`() = runTest {
            val result = dependanger {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                    library("spring-web", "org.springframework:spring-web:6.1.0")
                }
                compatibility {
                    versionConstraint("spring-same-version") {
                        libraries("spring-core", "spring-web")
                        constraint = VersionConstraintType.SAME_VERSION
                        severity = Severity.ERROR
                    }
                }
            }.process()

            assertThat(result.diagnostics.errors).noneMatch {
                it.message.contains("SAME_VERSION")
            }
        }

        @Test
        fun `same major constraint violated when major versions differ`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.example:lib-a:2.0.0")
                    library("lib-b", "com.example:lib-b:3.0.0")
                }
                compatibility {
                    versionConstraint("major-alignment") {
                        libraries("lib-a", "lib-b")
                        constraint = VersionConstraintType.SAME_MAJOR
                        severity = Severity.ERROR
                    }
                }
            }.process()

            assertThat(result.isSuccess).isFalse()
            assertThat(result.diagnostics.errors).anyMatch {
                it.message.contains("SAME_MAJOR") && it.message.contains("lib-a") && it.message.contains("lib-b")
            }
        }

        @Test
        fun `same major constraint passes when major versions match`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.example:lib-a:2.0.0")
                    library("lib-b", "com.example:lib-b:2.5.0")
                }
                compatibility {
                    versionConstraint("major-alignment") {
                        libraries("lib-a", "lib-b")
                        constraint = VersionConstraintType.SAME_MAJOR
                        severity = Severity.ERROR
                    }
                }
            }.process()

            assertThat(result.diagnostics.errors).noneMatch {
                it.message.contains("SAME_MAJOR")
            }
        }

        @Test
        fun `same major minor constraint violated when minor versions differ`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.example:lib-a:2.1.0")
                    library("lib-b", "com.example:lib-b:2.3.0")
                }
                compatibility {
                    versionConstraint("minor-alignment") {
                        libraries("lib-a", "lib-b")
                        constraint = VersionConstraintType.SAME_MAJOR_MINOR
                        severity = Severity.ERROR
                    }
                }
            }.process()

            assertThat(result.isSuccess).isFalse()
            assertThat(result.diagnostics.errors).anyMatch {
                it.message.contains("SAME_MAJOR_MINOR")
            }
        }

        @Test
        fun `version constraint skipped when fewer than two libraries present`() = runTest {
            val result = dependanger {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                }
                compatibility {
                    versionConstraint("spring-same-version") {
                        libraries("spring-core", "spring-web")
                        constraint = VersionConstraintType.SAME_VERSION
                        severity = Severity.ERROR
                    }
                }
            }.process()

            assertThat(result.diagnostics.errors).noneMatch {
                it.message.contains("SAME_VERSION")
            }
        }

        @Test
        fun `version constraint with custom message uses that message`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.example:lib-a:1.0.0")
                    library("lib-b", "com.example:lib-b:2.0.0")
                }
                compatibility {
                    versionConstraint("custom-msg-rule") {
                        libraries("lib-a", "lib-b")
                        constraint = VersionConstraintType.SAME_VERSION
                        severity = Severity.ERROR
                        message = "Libraries must use the same version for compatibility"
                    }
                }
            }.process()

            assertThat(result.diagnostics.errors).anyMatch {
                it.message.contains("Libraries must use the same version for compatibility")
            }
        }
    }

    @Nested
    inner class `Multiple rules combined` {

        @Test
        fun `multiple rules produce multiple diagnostics`() = runTest {
            val result = dependanger(jdk = 11) {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                    library("log4j", "org.apache.logging.log4j:log4j-core:2.23.0")
                    library("logback", "ch.qos.logback:logback-classic:1.5.6")
                }
                compatibility {
                    jdkRequirement("spring6-jdk17") {
                        matches = "org.springframework*"
                        minJdk = 17
                        severity = Severity.ERROR
                        message = "Spring 6 requires JDK 17+"
                    }
                    mutualExclusion("logging-conflict") {
                        libraries("log4j", "logback")
                        severity = Severity.WARNING
                        message = "Use only one logging framework"
                    }
                }
            }.process()

            assertThat(result.diagnostics.errors).anyMatch { it.message.contains("Spring 6 requires JDK 17+") }
            assertThat(result.diagnostics.warnings).anyMatch { it.message.contains("Use only one logging framework") }
        }

        @Test
        fun `rules with different severities produce diagnostics at correct levels`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.example:lib-a:1.0.0")
                    library("lib-b", "com.example:lib-b:2.0.0")
                    library("conflict-x", "com.conflict:x:1.0.0")
                    library("conflict-y", "com.conflict:y:1.0.0")
                }
                compatibility {
                    versionConstraint("version-error") {
                        libraries("lib-a", "lib-b")
                        constraint = VersionConstraintType.SAME_VERSION
                        severity = Severity.ERROR
                    }
                    mutualExclusion("conflict-warning") {
                        libraries("conflict-x", "conflict-y")
                        severity = Severity.WARNING
                        message = "Conflicting libraries detected"
                    }
                }
            }.process()

            assertThat(result.diagnostics.errors).isNotEmpty
            assertThat(result.diagnostics.warnings).isNotEmpty
            assertThat(result.diagnostics.errors).anyMatch { it.severity == Severity.ERROR }
            assertThat(result.diagnostics.warnings).anyMatch { it.severity == Severity.WARNING }
        }

        @Test
        fun `no compatibility rules produces no compatibility diagnostics`() = runTest {
            val result = dependanger {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                }
            }.process()

            assertThat(result.diagnostics.errors).noneMatch { it.code.startsWith("COMPAT_") }
            assertThat(result.diagnostics.warnings).noneMatch { it.code.startsWith("COMPAT_") }
        }

        @Test
        fun `jdk and version constraint violations combined`() = runTest {
            val result = dependanger(jdk = 11) {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                    library("spring-web", "org.springframework:spring-web:6.0.0")
                }
                compatibility {
                    jdkRequirement("spring6-jdk17") {
                        matches = "org.springframework*"
                        minJdk = 17
                        severity = Severity.ERROR
                        message = "Spring 6 requires JDK 17+"
                    }
                    versionConstraint("spring-same-version") {
                        libraries("spring-core", "spring-web")
                        constraint = VersionConstraintType.SAME_VERSION
                        severity = Severity.ERROR
                    }
                }
            }.process()

            assertThat(result.isSuccess).isFalse()
            assertThat(result.diagnostics.errors).hasSizeGreaterThanOrEqualTo(2)
            assertThat(result.diagnostics.errors).anyMatch { it.message.contains("Spring 6 requires JDK 17+") }
            assertThat(result.diagnostics.errors).anyMatch { it.message.contains("SAME_VERSION") }
        }
    }

    @Nested
    inner class `Diagnostic code format` {

        @Test
        fun `compatibility diagnostics use COMPAT prefix with rule name`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.example:lib-a:1.0.0")
                    library("lib-b", "com.example:lib-b:1.0.0")
                }
                compatibility {
                    mutualExclusion("my-exclusion-rule") {
                        libraries("lib-a", "lib-b")
                        severity = Severity.WARNING
                        message = "Conflicting"
                    }
                }
            }.process()

            assertThat(result.diagnostics.warnings).anyMatch {
                it.code.startsWith("COMPAT_") && it.code.contains("my-exclusion-rule", ignoreCase = true)
            }
        }
    }
}
