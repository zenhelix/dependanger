package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ComplexCatalogBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        jdk: Int? = null,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) {
        preset(preset)
        jdk?.let { jdkVersion(it) }
    }

    private fun bomConfig(
        groupId: String = "io.github.zenhelix",
        artifactId: String = "test-bom",
        version: String = "1.0.0",
    ): BomConfig = BomConfig(
        groupId = groupId, artifactId = artifactId, version = version,
        name = null, description = null, filename = "bom.pom.xml",
        includeOptionalDependencies = false, prettyPrint = true,
        includeDeprecationComments = true,
    )

    private fun realisticCatalogDsl(): DependangerDsl.() -> Unit = {
        versions {
            version("kotlin", "2.1.20")
            version("ktor", "3.1.1")
            version("spring", "6.1.0")
            version("junit", "5.11.0")
            version("assertj", "3.27.3")
        }
        libraries {
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin")) {
                tags("kotlin")
            }
            library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) {
                tags("ktor")
            }
            library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor")) {
                tags("ktor")
            }
            library("ktor-json", "io.ktor:ktor-serialization-json", versionRef("ktor")) {
                tags("ktor")
            }
            library("spring-core", "org.springframework:spring-core", versionRef("spring")) {
                tags("spring")
            }
            library("spring-web", "org.springframework:spring-web", versionRef("spring")) {
                tags("spring")
            }
            library("junit-jupiter", "org.junit.jupiter:junit-jupiter", versionRef("junit")) {
                tags("test")
            }
            library("assertj-core", "org.assertj:assertj-core", versionRef("assertj")) {
                tags("test")
            }
        }
        bundles {
            bundle("ktor") { libraries("ktor-core", "ktor-cio", "ktor-json") }
            bundle("spring") { libraries("spring-core", "spring-web") }
            bundle("testing") { libraries("junit-jupiter", "assertj-core") }
        }
        distributions {
            distribution("backend") {
                spec { byTags { include { anyOf("kotlin", "ktor", "spring") } } }
            }
            distribution("testing") {
                spec { byTags { include { anyOf("test") } } }
            }
        }
    }

    @Nested
    inner class `Realistic catalog with multiple distributions` {

        @Test
        fun `full catalog processes correctly`() = runTest {
            val result = dependanger(dslBlock = realisticCatalogDsl()).process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(8)
            assertThat((result as DependangerResult.Success).effective.libraries.keys).containsExactlyInAnyOrder(
                "kotlin-stdlib", "ktor-core", "ktor-cio", "ktor-json",
                "spring-core", "spring-web", "junit-jupiter", "assertj-core",
            )
            assertThat((result as DependangerResult.Success).effective.bundles).hasSize(3)
            assertThat((result as DependangerResult.Success).effective.bundles.keys)
                .containsExactlyInAnyOrder("ktor", "spring", "testing")
        }

        @Test
        fun `backend distribution filters correctly`() = runTest {
            val result = dependanger(dslBlock = realisticCatalogDsl())
                .process(distribution = "backend")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(6)
            assertThat((result as DependangerResult.Success).effective.libraries.keys).containsExactlyInAnyOrder(
                "kotlin-stdlib", "ktor-core", "ktor-cio", "ktor-json",
                "spring-core", "spring-web",
            )
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("junit-jupiter")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("assertj-core")
        }

        @Test
        fun `testing distribution filters correctly`() = runTest {
            val result = dependanger(dslBlock = realisticCatalogDsl())
                .process(distribution = "testing")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(2)
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("junit-jupiter", "assertj-core")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("kotlin-stdlib")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("ktor-core")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("spring-core")
        }
    }

    @Nested
    inner class `Platform library in full pipeline` {

        @Test
        fun `platform library flows through to TOML and BOM`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    platformLibrary("kotlin-bom", "org.jetbrains.kotlin:kotlin-bom", versionRef("kotlin"))
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("kotlin-bom")
            assertThat((result as DependangerResult.Success).effective.libraries["kotlin-bom"]!!.isPlatform).isTrue()

            val toml = result.toToml()
            assertThat(toml).contains("[libraries]")
            assertThat(toml).contains("kotlin-bom")
            assertThat(toml).contains("kotlin-stdlib")

            val bom = result.toBom(bomConfig())
            assertThat(bom).contains("<artifactId>kotlin-bom</artifactId>")
            assertThat(bom).contains("<type>pom</type>")
            assertThat(bom).contains("<scope>import</scope>")
            assertThat(bom).contains("<artifactId>kotlin-stdlib</artifactId>")
        }
    }

    @Nested
    inner class `Deep bundle extends chain` {

        @Test
        fun `three-level bundle extends resolves all libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.a:lib:1.0")
                    library("lib-b", "com.b:lib:1.0")
                    library("lib-c", "com.c:lib:1.0")
                    library("lib-d", "com.d:lib:1.0")
                }
                bundles {
                    bundle("base-bundle") { libraries("lib-a", "lib-b") }
                    bundle("mid-bundle") {
                        libraries("lib-c")
                        extends("base-bundle")
                    }
                    bundle("full-bundle") {
                        libraries("lib-d")
                        extends("mid-bundle")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.bundles["full-bundle"]!!.libraries)
                .containsExactlyInAnyOrder("lib-a", "lib-b", "lib-c", "lib-d")
        }

        @Test
        fun `diamond bundle extends deduplicates libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-base", "com.base:lib:1.0")
                    library("lib-a", "com.a:lib:1.0")
                    library("lib-b", "com.b:lib:1.0")
                }
                bundles {
                    bundle("base") { libraries("lib-base") }
                    bundle("branch-a") {
                        libraries("lib-a")
                        extends("base")
                    }
                    bundle("branch-b") {
                        libraries("lib-b")
                        extends("base")
                    }
                    bundle("all") {
                        extends("branch-a", "branch-b")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val allLibraries = (result as DependangerResult.Success).effective.bundles["all"]!!.libraries
            assertThat(allLibraries).containsExactlyInAnyOrder("lib-base", "lib-a", "lib-b")
            assertThat(allLibraries.size).isEqualTo(allLibraries.toSet().size)
        }
    }

    @Nested
    inner class `Full pipeline to TOML` {

        @Test
        fun `DSL to TOML end-to-end with all sections`() = runTest {
            val result = dependanger {
                versions {
                    version("kotlin", "2.1.20")
                    version("ktor", "3.1.1")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                    library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                    library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
                }
                plugins {
                    plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin"))
                }
                bundles {
                    bundle("ktor") { libraries("ktor-core", "ktor-cio") }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()

            assertThat(toml).contains("[versions]")
            assertThat(toml).contains("kotlin = \"2.1.20\"")
            assertThat(toml).contains("ktor = \"3.1.1\"")

            assertThat(toml).contains("[libraries]")
            assertThat(toml).contains("kotlin-stdlib")
            assertThat(toml).contains("ktor-core")
            assertThat(toml).contains("ktor-cio")

            assertThat(toml).contains("[plugins]")
            assertThat(toml).contains("kotlin-jvm")
            assertThat(toml).contains("org.jetbrains.kotlin.jvm")

            assertThat(toml).contains("[bundles]")
            assertThat(toml).contains("ktor")
            assertThat(toml).contains("\"ktor-core\"")
            assertThat(toml).contains("\"ktor-cio\"")
        }

        @Test
        fun `distribution-specific TOML has correct content`() = runTest {
            val result = dependanger(dslBlock = realisticCatalogDsl())
                .process(distribution = "backend")

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()

            assertThat(toml).contains("kotlin-stdlib")
            assertThat(toml).contains("ktor-core")
            assertThat(toml).contains("spring-core")
            assertThat(toml).doesNotContain("junit-jupiter")
            assertThat(toml).doesNotContain("assertj-core")

            assertThat(toml).contains("kotlin = \"2.1.20\"")
            assertThat(toml).contains("ktor = \"3.1.1\"")
            assertThat(toml).contains("spring = \"6.1.0\"")
        }
    }

    @Nested
    inner class `Full pipeline to BOM` {

        @Test
        fun `DSL to BOM end-to-end`() = runTest {
            val result = dependanger(dslBlock = realisticCatalogDsl()).process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig())

            assertThat(bom).contains("<groupId>io.github.zenhelix</groupId>")
            assertThat(bom).contains("<artifactId>test-bom</artifactId>")
            assertThat(bom).contains("<version>1.0.0</version>")
            assertThat(bom).contains("<packaging>pom</packaging>")
            assertThat(bom).contains("<dependencyManagement>")

            assertThat(bom).contains("<groupId>org.jetbrains.kotlin</groupId>")
            assertThat(bom).contains("<artifactId>kotlin-stdlib</artifactId>")
            assertThat(bom).contains("<version>2.1.20</version>")

            assertThat(bom).contains("<groupId>io.ktor</groupId>")
            assertThat(bom).contains("<artifactId>ktor-client-core</artifactId>")
            assertThat(bom).contains("<version>3.1.1</version>")

            assertThat(bom).contains("<groupId>org.springframework</groupId>")
            assertThat(bom).contains("<artifactId>spring-core</artifactId>")
            assertThat(bom).contains("<version>6.1.0</version>")

            assertThat(bom).contains("<groupId>org.junit.jupiter</groupId>")
            assertThat(bom).contains("<artifactId>junit-jupiter</artifactId>")
            assertThat(bom).contains("<version>5.11.0</version>")

            assertThat(bom).contains("<groupId>org.assertj</groupId>")
            assertThat(bom).contains("<artifactId>assertj-core</artifactId>")
            assertThat(bom).contains("<version>3.27.3</version>")
        }

        @Test
        fun `distribution-specific BOM has correct content`() = runTest {
            val result = dependanger(dslBlock = realisticCatalogDsl())
                .process(distribution = "backend")

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig())

            assertThat(bom).contains("<artifactId>kotlin-stdlib</artifactId>")
            assertThat(bom).contains("<artifactId>ktor-client-core</artifactId>")
            assertThat(bom).contains("<artifactId>spring-core</artifactId>")
            assertThat(bom).doesNotContain("<artifactId>junit-jupiter</artifactId>")
            assertThat(bom).doesNotContain("<artifactId>assertj-core</artifactId>")
        }
    }

    @Nested
    inner class `Validate then process` {

        @Test
        fun `validate before process catches errors early`() = runTest {
            val instance = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
            }

            val validation = instance.validate()
            assertThat(validation.isSuccess).isTrue()
            assertThat(validation.diagnostics.hasErrors).isFalse()

            val result = instance.process()
            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("kotlin-stdlib")
        }

        @Test
        fun `validate catches errors without full processing`() = runTest {
            val instance = dependanger {
                libraries {
                    library("broken-lib", "com.example:broken", versionRef("nonexistent"))
                }
            }

            val validation = instance.validate()
            assertThat(validation.diagnostics.hasErrors).isTrue()
        }
    }

    @Nested
    inner class `Multiple sequential processes` {

        @Test
        fun `same Dependanger instance produces consistent results`() = runTest {
            val instance = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                }
            }

            val result1 = instance.process()
            val result2 = instance.process()

            assertThat(result1.isSuccess).isTrue()
            assertThat(result2.isSuccess).isTrue()
            assertThat((result1 as DependangerResult.Success).effective.libraries.keys)
                .isEqualTo((result2 as DependangerResult.Success).effective.libraries.keys)
            assertThat((result1 as DependangerResult.Success).effective.libraries["kotlin-stdlib"]!!.version.valueOrNull)
                .isEqualTo((result2 as DependangerResult.Success).effective.libraries["kotlin-stdlib"]!!.version.valueOrNull)
            assertThat((result1 as DependangerResult.Success).effective.libraries["ktor-core"]!!.version.valueOrNull)
                .isEqualTo((result2 as DependangerResult.Success).effective.libraries["ktor-core"]!!.version.valueOrNull)
        }
    }

    @Nested
    inner class `Large catalog performance` {

        @Test
        fun `catalog with 50 libraries processes successfully`() = runTest {
            val result = dependanger {
                versions {
                    (1..50).forEach { i -> version("v$i", "$i.0.0") }
                }
                libraries {
                    (1..50).forEach { i ->
                        library("lib-$i", "com.example.group$i:artifact-$i", versionRef("v$i"))
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).hasSize(50)
            (1..50).forEach { i ->
                assertThat((result as DependangerResult.Success).effective.libraries).containsKey("lib-$i")
                assertThat((result as DependangerResult.Success).effective.libraries["lib-$i"]!!.version.valueOrNull)
                    .isEqualTo("$i.0.0")
            }
        }
    }
}
