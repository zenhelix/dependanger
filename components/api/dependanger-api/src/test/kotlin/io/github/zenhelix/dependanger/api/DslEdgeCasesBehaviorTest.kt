package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DslEdgeCasesBehaviorTest {

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
        prettyPrint: Boolean = true,
    ): BomConfig = BomConfig(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        name = null,
        description = null,
        filename = "bom.pom.xml",
        includeOptionalDependencies = false,
        prettyPrint = prettyPrint,
        includeDeprecationComments = false,
    )

    @Nested
    inner class `Duplicate declarations` {

        @Test
        fun `duplicate version alias keeps last declared value`() = runTest {
            val result = dependanger {
                versions {
                    version("kotlin", "2.0.0")
                    version("kotlin", "2.1.20")
                }
                libraries { library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.versions["kotlin"]!!.value).isEqualTo("2.1.20")
            assertThat(result.effective!!.libraries["kotlin-stdlib"]!!.version!!.value).isEqualTo("2.1.20")
        }

        @Test
        fun `duplicate library alias keeps last declared value`() = runTest {
            val result = dependanger {
                libraries {
                    library("my-lib", "com.old:old-artifact:1.0.0")
                    library("my-lib", "com.new:new-artifact:2.0.0")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries).hasSize(1)
            assertThat(result.effective!!.libraries).containsKey("my-lib")
            assertThat(result.effective!!.libraries["my-lib"]!!.group).isEqualTo("com.new")
            assertThat(result.effective!!.libraries["my-lib"]!!.artifact).isEqualTo("new-artifact")
        }

        @Test
        fun `duplicate bundle alias keeps last declared value`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.a:lib:1.0")
                    library("lib-b", "com.b:lib:1.0")
                    library("lib-c", "com.c:lib:1.0")
                }
                bundles {
                    bundle("my-bundle") { libraries("lib-a") }
                    bundle("my-bundle") { libraries("lib-b", "lib-c") }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.bundles).hasSize(1)
            assertThat(result.effective!!.bundles).containsKey("my-bundle")
            assertThat(result.effective!!.bundles["my-bundle"]!!.libraries)
                .containsExactlyInAnyOrder("lib-b", "lib-c")
        }

        @Test
        fun `duplicate plugin alias keeps last declared value`() = runTest {
            val result = dependanger {
                versions {
                    version("v1", "1.0.0")
                    version("v2", "2.0.0")
                }
                plugins {
                    plugin("my-plugin", "com.old.plugin", versionRef("v1"))
                    plugin("my-plugin", "com.new.plugin", versionRef("v2"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.plugins).hasSize(1)
            assertThat(result.effective!!.plugins).containsKey("my-plugin")
            assertThat(result.effective!!.plugins["my-plugin"]!!.id).isEqualTo("com.new.plugin")
            assertThat(result.effective!!.plugins["my-plugin"]!!.version!!.value).isEqualTo("2.0.0")
        }
    }

    @Nested
    inner class `Empty blocks` {

        @Test
        fun `completely empty DSL produces valid empty result`() = runTest {
            val result = dependanger {}.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries).isEmpty()
            assertThat(result.effective!!.versions).isEmpty()
            assertThat(result.effective!!.bundles).isEmpty()
            assertThat(result.effective!!.plugins).isEmpty()
        }

        @Test
        fun `empty versions block with non-empty libraries`() = runTest {
            val result = dependanger {
                versions { }
                libraries {
                    library("assertj", "org.assertj:assertj-core:3.27.3")
                    library("guava", "com.google.guava:guava:33.0-jre")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries).hasSize(2)
            assertThat(result.effective!!.libraries).containsKey("assertj")
            assertThat(result.effective!!.libraries).containsKey("guava")
            assertThat(result.effective!!.libraries["assertj"]!!.version!!.value).isEqualTo("3.27.3")
        }

        @Test
        fun `DSL with only bundles referencing non-existent libraries`() = runTest {
            val result = dependanger {
                bundles {
                    bundle("phantom") { libraries("non-existent-a", "non-existent-b") }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries).isEmpty()
            assertThat(result.effective!!.bundles).isEmpty()
        }
    }

    @Nested
    inner class `Version-less libraries` {

        @Test
        fun `library without version has null version in effective`() = runTest {
            val result = dependanger {
                libraries { library("bom-managed", "com.example:bom-managed") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries).containsKey("bom-managed")
            assertThat(result.effective!!.libraries["bom-managed"]!!.version).isNull()
            assertThat(result.effective!!.libraries["bom-managed"]!!.group).isEqualTo("com.example")
            assertThat(result.effective!!.libraries["bom-managed"]!!.artifact).isEqualTo("bom-managed")
        }

        @Test
        fun `version-less library appears in TOML without version field`() = runTest {
            val result = dependanger {
                libraries { library("bom-managed", "com.example:bom-managed") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("bom-managed")
            assertThat(toml).contains("com.example")
            assertThat(toml).doesNotContain("version.ref")
            assertThat(toml).doesNotContain("version =")
        }

        @Test
        fun `version-less library works alongside versioned libraries`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                    library("bom-managed", "com.example:bom-managed")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries).hasSize(2)
            assertThat(result.effective!!.libraries["kotlin-stdlib"]!!.version!!.value).isEqualTo("2.1.20")
            assertThat(result.effective!!.libraries["bom-managed"]!!.version).isNull()
        }
    }

    @Nested
    inner class `Library coordinate formats` {

        @Test
        fun `full coordinate group-artifact-version extracts version`() = runTest {
            val result = dependanger {
                libraries { library("assertj", "org.assertj:assertj-core:3.27.3") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = result.effective!!.libraries["assertj"]!!
            assertThat(lib.group).isEqualTo("org.assertj")
            assertThat(lib.artifact).isEqualTo("assertj-core")
            assertThat(lib.version!!.value).isEqualTo("3.27.3")
        }

        @Test
        fun `partial coordinate group-artifact works without version`() = runTest {
            val result = dependanger {
                libraries { library("managed-lib", "com.example:managed-lib") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = result.effective!!.libraries["managed-lib"]!!
            assertThat(lib.group).isEqualTo("com.example")
            assertThat(lib.artifact).isEqualTo("managed-lib")
            assertThat(lib.version).isNull()
        }

        @Test
        fun `library with version ref resolves to declared version`() = runTest {
            val result = dependanger {
                versions { version("ktor", "3.1.1") }
                libraries { library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = result.effective!!.libraries["ktor-core"]!!
            assertThat(lib.group).isEqualTo("io.ktor")
            assertThat(lib.artifact).isEqualTo("ktor-client-core")
            assertThat(lib.version!!.value).isEqualTo("3.1.1")
        }
    }

    @Nested
    inner class `Platform library behavior` {

        @Test
        fun `platform library has isPlatform true`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    platformLibrary("kotlin-bom", "org.jetbrains.kotlin:kotlin-bom", versionRef("kotlin"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = result.effective!!.libraries["kotlin-bom"]!!
            assertThat(lib.isPlatform).isTrue()
            assertThat(lib.group).isEqualTo("org.jetbrains.kotlin")
            assertThat(lib.artifact).isEqualTo("kotlin-bom")
            assertThat(lib.version!!.value).isEqualTo("2.1.20")
        }

        @Test
        fun `platform library produces type pom and scope import in BOM`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    platformLibrary("kotlin-bom", "org.jetbrains.kotlin:kotlin-bom", versionRef("kotlin"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig())

            assertThat(bom).contains("<type>pom</type>")
            assertThat(bom).contains("<scope>import</scope>")
            assertThat(bom).contains("<groupId>org.jetbrains.kotlin</groupId>")
            assertThat(bom).contains("<artifactId>kotlin-bom</artifactId>")
        }

        @Test
        fun `platform and regular libraries coexist`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    platformLibrary("kotlin-bom", "org.jetbrains.kotlin:kotlin-bom", versionRef("kotlin"))
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries).hasSize(2)
            assertThat(result.effective!!.libraries["kotlin-bom"]!!.isPlatform).isTrue()
            assertThat(result.effective!!.libraries["kotlin-stdlib"]!!.isPlatform).isFalse()

            val bom = result.toBom(bomConfig())
            assertThat(bom).contains("<type>pom</type>")
            assertThat(bom).contains("<groupId>org.jetbrains.kotlin</groupId>")
        }
    }

    @Nested
    inner class `Tags and metadata` {

        @Test
        fun `multiple tags preserved in effective`() = runTest {
            val result = dependanger {
                libraries {
                    library("multi-tagged", "com.example:multi-tagged:1.0.0") {
                        tags("backend", "jvm", "production", "core")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = result.effective!!.libraries["multi-tagged"]!!
            assertThat(lib.tags).hasSize(4)
            assertThat(lib.tags).containsExactlyInAnyOrder("backend", "jvm", "production", "core")
        }

        @Test
        fun `tags filter correctly in distribution`() = runTest {
            val result = dependanger {
                libraries {
                    library("backend-lib", "com.example:backend:1.0") { tags("backend", "jvm") }
                    library("frontend-lib", "com.example:frontend:1.0") { tags("frontend", "js") }
                    library("shared-lib", "com.example:shared:1.0") { tags("backend", "frontend") }
                }
                distributions {
                    distribution("backend") {
                        spec { byTags { include { anyOf("backend") } } }
                    }
                }
            }.process(distribution = "backend")

            assertThat(result.isSuccess).isTrue()
            assertThat(result.effective!!.libraries).hasSize(2)
            assertThat(result.effective!!.libraries).containsKey("backend-lib")
            assertThat(result.effective!!.libraries).containsKey("shared-lib")
            assertThat(result.effective!!.libraries).doesNotContainKey("frontend-lib")
        }

        @Test
        fun `library with empty tags has empty set`() = runTest {
            val result = dependanger {
                libraries {
                    library("no-tags", "com.example:no-tags:1.0.0")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val lib = result.effective!!.libraries["no-tags"]!!
            assertThat(lib.tags).isEmpty()
            assertThat(lib.group).isEqualTo("com.example")
            assertThat(lib.artifact).isEqualTo("no-tags")
        }
    }
}
