package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TomlGenerationBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) { preset(preset) }

    @Nested
    inner class `Version catalog structure` {

        @Test
        fun `TOML contains all four sections`() = runTest {
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
        fun `empty sections are omitted`() = runTest {
            val result = dependanger {
                libraries { library("assertj", "org.assertj:assertj-core:3.27.3") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("[libraries]")
            assertThat(toml).doesNotContain("[plugins]")
            assertThat(toml).doesNotContain("[bundles]")
        }

        @Test
        fun `versions section uses correct TOML format`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries { library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("kotlin = \"2.1.20\"")
        }

        @Test
        fun `libraries use version ref by default`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries { library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains(
                "kotlin-stdlib = { group = \"org.jetbrains.kotlin\", name = \"kotlin-stdlib\", version.ref = \"kotlin\" }"
            )
        }
    }

    @Nested
    inner class `Deprecation comments` {

        @Test
        fun `deprecated library has deprecation comment`() = runTest {
            val result = dependanger {
                libraries {
                    library("old-lib", "com.example:old-lib:1.0.0") {
                        deprecated(replacedBy = "new-lib", message = "Use new-lib instead")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("# DEPRECATED:")
            assertThat(toml).contains("Use new-lib instead")
        }

        @Test
        fun `deprecation comments can be disabled`() = runTest {
            val result = dependanger {
                libraries {
                    library("old-lib", "com.example:old-lib:1.0.0") {
                        deprecated(replacedBy = "new-lib", message = "Use new-lib instead")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml(
                TomlConfig(
                    filename = "libs.versions.toml",
                    includeComments = true,
                    sortSections = true,
                    useInlineVersions = false,
                    includeDeprecationComments = false,
                )
            )
            assertThat(toml).doesNotContain("DEPRECATED")
        }

        @Test
        fun `deprecated library with removal version shows it`() = runTest {
            val result = dependanger {
                libraries {
                    library("old-lib", "com.example:old-lib:1.0.0") {
                        deprecated {
                            replacedBy = "new-lib"
                            message = "Migrating to new-lib"
                            removalVersion = "3.0.0"
                        }
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("DEPRECATED")
            assertThat(toml).contains("Removal: 3.0.0")
        }
    }

    @Nested
    inner class `Inline versions` {

        @Test
        fun `inline versions embed version directly in library`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries { library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml(
                TomlConfig(
                    filename = "libs.versions.toml",
                    includeComments = true,
                    sortSections = true,
                    useInlineVersions = true,
                    includeDeprecationComments = true,
                )
            )
            assertThat(toml).contains("version = \"2.1.20\"")
            assertThat(toml).doesNotContain("version.ref")
        }

        @Test
        fun `default config uses version ref not inline`() = runTest {
            val result = dependanger {
                versions { version("ktor", "3.1.1") }
                libraries { library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("version.ref = \"ktor\"")
            assertThat(toml).doesNotContain("version = \"3.1.1\"")
        }
    }

    @Nested
    inner class `Sorted sections` {

        @Test
        fun `sorted sections alphabetize entries`() = runTest {
            val result = dependanger {
                versions {
                    version("ktor", "3.1.1")
                    version("assertj", "3.27.3")
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                    library("assertj", "org.assertj:assertj-core", versionRef("assertj"))
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml(
                TomlConfig(
                    filename = "libs.versions.toml",
                    includeComments = true,
                    sortSections = true,
                    useInlineVersions = false,
                    includeDeprecationComments = true,
                )
            )

            val lines = toml.lines()
            val versionLines = lines
                .dropWhile { it != "[versions]" }.drop(1)
                .takeWhile { it.isNotBlank() && !it.startsWith("[") }

            val versionAliases = versionLines.map { it.substringBefore(" =") }
            assertThat(versionAliases).isSorted()

            val libraryLines = lines
                .dropWhile { it != "[libraries]" }.drop(1)
                .takeWhile { it.isNotBlank() && !it.startsWith("[") }

            val libraryAliases = libraryLines.map { it.substringBefore(" =") }
            assertThat(libraryAliases).isSorted()
        }

        @Test
        fun `unsorted sections preserve declaration order`() = runTest {
            val result = dependanger {
                versions {
                    version("ktor", "3.1.1")
                    version("assertj", "3.27.3")
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                    library("assertj", "org.assertj:assertj-core", versionRef("assertj"))
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml(
                TomlConfig(
                    filename = "libs.versions.toml",
                    includeComments = true,
                    sortSections = false,
                    useInlineVersions = false,
                    includeDeprecationComments = true,
                )
            )

            val lines = toml.lines()
            val versionLines = lines
                .dropWhile { it != "[versions]" }.drop(1)
                .takeWhile { it.isNotBlank() && !it.startsWith("[") }

            val versionAliases = versionLines.map { it.substringBefore(" =") }
            assertThat(versionAliases).containsExactly("ktor", "assertj", "kotlin")
        }
    }

    @Nested
    inner class `Library without version` {

        @Test
        fun `library without version omits version field`() = runTest {
            val result = dependanger {
                libraries { library("bom-managed", "com.example:bom-managed") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("bom-managed = { group = \"com.example\", name = \"bom-managed\" }")
            assertThat(toml).doesNotContain("version.ref")
        }
    }

    @Nested
    inner class `Bundle format` {

        @Test
        fun `bundles list library aliases`() = runTest {
            val result = dependanger {
                versions { version("ktor", "3.1.1") }
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                    library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
                    library("ktor-json", "io.ktor:ktor-serialization-json", versionRef("ktor"))
                }
                bundles { bundle("ktor") { libraries("ktor-core", "ktor-cio", "ktor-json") } }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("[bundles]")
            assertThat(toml).contains("ktor = [\"ktor-c")
            assertThat(toml).contains("\"ktor-core\"")
            assertThat(toml).contains("\"ktor-cio\"")
            assertThat(toml).contains("\"ktor-json\"")
        }
    }

    @Nested
    inner class `Plugin format` {

        @Test
        fun `plugins use id and version ref`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                plugins { plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains(
                "kotlin-jvm = { id = \"org.jetbrains.kotlin.jvm\", version.ref = \"kotlin\" }"
            )
        }
    }

    @Nested
    inner class `Comments header` {

        @Test
        fun `TOML includes generated header comment`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml(
                TomlConfig(
                    filename = "libs.versions.toml",
                    includeComments = true,
                    sortSections = true,
                    useInlineVersions = false,
                    includeDeprecationComments = true,
                )
            )
            assertThat(toml.lines().first()).startsWith("#")
            assertThat(toml).contains("Generated by Dependanger")
        }

        @Test
        fun `header can be disabled`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml(
                TomlConfig(
                    filename = "libs.versions.toml",
                    includeComments = false,
                    sortSections = true,
                    useInlineVersions = false,
                    includeDeprecationComments = true,
                )
            )
            assertThat(toml).doesNotContain("Generated by Dependanger")
            assertThat(toml.lines().first()).doesNotStartWith("#")
        }
    }

    @Nested
    inner class `Distribution-specific TOML` {

        @Test
        fun `TOML from distribution contains only filtered libraries`() = runTest {
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
            val toml = result.toToml()
            assertThat(toml).contains("android-lib")
            assertThat(toml).contains("common-lib")
            assertThat(toml).doesNotContain("server-lib")
        }
    }
}
