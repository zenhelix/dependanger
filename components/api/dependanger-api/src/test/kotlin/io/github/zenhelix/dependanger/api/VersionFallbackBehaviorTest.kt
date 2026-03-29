package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VersionFallbackBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        jdk: Int? = null,
        kotlin: String? = null,
        gradle: String? = null,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) {
        preset(preset)
        jdk?.let { jdkVersion(it) }
        kotlin?.let { kotlinVersion(it) }
        gradle?.let { gradleVersion(it) }
    }

    @Nested
    inner class `Kotlin version fallback` {

        @Test
        fun `fallback applies when kotlin version matches condition`() = runTest {
            val result = dependanger(kotlin = "1.9.0") {
                versions {
                    version("lib", "2.0") {
                        fallback("1.5") { kotlinVersionBelow("2.0.0") }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("1.5")
        }

        @Test
        fun `fallback does not apply when kotlin version exceeds condition`() = runTest {
            val result = dependanger(kotlin = "2.1.0") {
                versions {
                    version("lib", "2.0") {
                        fallback("1.5") { kotlinVersionBelow("2.0.0") }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("2.0")
        }
    }

    @Nested
    inner class `Gradle version fallback` {

        @Test
        fun `fallback applies for old gradle version`() = runTest {
            val result = dependanger(gradle = "7.6") {
                versions {
                    version("lib", "9.0") {
                        fallback("8.0") { gradleVersionBelow("8.0") }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("8.0")
        }

        @Test
        fun `fallback does not apply for new gradle version`() = runTest {
            val result = dependanger(gradle = "8.5") {
                versions {
                    version("lib", "9.0") {
                        fallback("8.0") { gradleVersionBelow("8.0") }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("9.0")
        }
    }

    @Nested
    inner class `Multiple fallbacks with priority` {

        @Test
        fun `first matching fallback wins`() = runTest {
            val result = dependanger(jdk = 7) {
                versions {
                    version("lib", "5.0") {
                        fallback("android") { jdkBelow(11) }
                        fallback("legacy") { jdkBelow(8) }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("android")
        }

        @Test
        fun `second fallback matches when first does not`() = runTest {
            val result = dependanger(jdk = 10) {
                versions {
                    version("lib", "5.0") {
                        fallback("for-jdk-8") { jdkBelow(9) }
                        fallback("for-jdk-11") { jdkBelow(12) }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("for-jdk-11")
        }
    }

    @Nested
    inner class `Composite conditions` {

        @Test
        fun `all combinator requires all conditions to match`() = runTest {
            val result = dependanger(jdk = 17, kotlin = "2.0.0") {
                versions {
                    version("lib", "1.0") {
                        fallback("2.0") { all(jdkAtLeast(11), kotlinVersionAtLeast("1.9.0")) }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("2.0")
        }

        @Test
        fun `all combinator fails when one condition does not match`() = runTest {
            val result = dependanger(jdk = 17, kotlin = "1.8.0") {
                versions {
                    version("lib", "1.0") {
                        fallback("2.0") { all(jdkAtLeast(11), kotlinVersionAtLeast("1.9.0")) }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("1.0")
        }

        @Test
        fun `any combinator matches when at least one condition matches`() = runTest {
            val result = dependanger(jdk = 8, kotlin = "2.0.0") {
                versions {
                    version("lib", "1.0") {
                        fallback("compat") { any(jdkBelow(11), kotlinVersionBelow("1.9.0")) }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("compat")
        }

        @Test
        fun `not combinator inverts condition - fallback applies when negated condition is true`() = runTest {
            val result = dependanger(jdk = 11) {
                versions {
                    version("lib", "1.0") {
                        fallback("non-lts") { not(jdkAtLeast(17)) }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("non-lts")
        }

        @Test
        fun `not combinator inverts condition - fallback does not apply when negated condition is false`() = runTest {
            val result = dependanger(jdk = 21) {
                versions {
                    version("lib", "1.0") {
                        fallback("non-lts") { not(jdkAtLeast(17)) }
                    }
                }
                libraries { library("lib", "com.example:lib", versionRef("lib")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["lib"]!!.version!!.value).isEqualTo("1.0")
        }
    }

    @Nested
    inner class `Fallback with library resolution` {

        @Test
        fun `fallback version is used in library version resolution`() = runTest {
            val result = dependanger(jdk = 8) {
                versions {
                    version("guava", "33.0-jre") {
                        fallback("33.0-android") { jdkBelow(11) }
                    }
                }
                libraries { library("guava", "com.google.guava:guava", versionRef("guava")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = result.effective!!.libraries["guava"]!!
            assertThat(lib.version!!.value).isEqualTo("33.0-android")
        }

        @Test
        fun `multiple libraries sharing a fallback version all get the fallback value`() = runTest {
            val result = dependanger(jdk = 8) {
                versions {
                    version("guava", "33.0-jre") {
                        fallback("33.0-android") { jdkBelow(11) }
                    }
                }
                libraries {
                    library("guava-core", "com.google.guava:guava", versionRef("guava"))
                    library("guava-testlib", "com.google.guava:guava-testlib", versionRef("guava"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries["guava-core"]!!.version!!.value)
                .isEqualTo("33.0-android")
            assertThat(result.effective!!.libraries["guava-testlib"]!!.version!!.value)
                .isEqualTo("33.0-android")
        }
    }
}
