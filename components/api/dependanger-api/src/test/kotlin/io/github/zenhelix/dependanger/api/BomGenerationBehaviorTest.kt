package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BomGenerationBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) { preset(preset) }

    private fun bomConfig(
        groupId: String = "io.github.zenhelix",
        artifactId: String = "test-bom",
        version: String = "1.0.0",
        name: String? = null,
        description: String? = null,
        prettyPrint: Boolean = true,
        includeDeprecationComments: Boolean = true,
        includeOptionalDependencies: Boolean = false,
    ): BomConfig = BomConfig(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        name = name,
        description = description,
        filename = "bom.pom.xml",
        includeOptionalDependencies = includeOptionalDependencies,
        prettyPrint = prettyPrint,
        includeDeprecationComments = includeDeprecationComments,
    )

    @Nested
    inner class `BOM XML structure` {

        @Test
        fun `BOM contains project coordinates`() = runTest {
            val result = dependanger {
                libraries { library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig())

            assertThat(bom).contains("<groupId>io.github.zenhelix</groupId>")
            assertThat(bom).contains("<artifactId>test-bom</artifactId>")
            assertThat(bom).contains("<version>1.0.0</version>")
            assertThat(bom).contains("<packaging>pom</packaging>")
        }

        @Test
        fun `BOM contains dependencyManagement section`() = runTest {
            val result = dependanger {
                libraries { library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig())

            assertThat(bom).contains("<dependencyManagement>")
            assertThat(bom).contains("<dependencies>")
        }

        @Test
        fun `each library appears as dependency`() = runTest {
            val result = dependanger {
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                    library("assertj", "org.assertj:assertj-core:3.27.3")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig())

            assertThat(bom).contains("<groupId>org.jetbrains.kotlin</groupId>")
            assertThat(bom).contains("<artifactId>kotlin-stdlib</artifactId>")
            assertThat(bom).contains("<version>2.1.20</version>")

            assertThat(bom).contains("<groupId>io.ktor</groupId>")
            assertThat(bom).contains("<artifactId>ktor-client-core</artifactId>")
            assertThat(bom).contains("<version>3.1.1</version>")

            assertThat(bom).contains("<groupId>org.assertj</groupId>")
            assertThat(bom).contains("<artifactId>assertj-core</artifactId>")
            assertThat(bom).contains("<version>3.27.3</version>")
        }

        @Test
        fun `BOM uses resolved versions not version refs`() = runTest {
            val result = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig())

            assertThat(bom).contains("<version>2.1.20</version>")
            assertThat(bom).doesNotContain("versionRef")
            assertThat(bom).doesNotContain("\"kotlin\"")
        }
    }

    @Nested
    inner class `BOM metadata` {

        @Test
        fun `name and description in BOM`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig(name = "Test BOM", description = "For testing"))

            assertThat(bom).contains("<name>Test BOM</name>")
            assertThat(bom).contains("<description>For testing</description>")
        }

        @Test
        fun `name and description omitted when null`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig())

            assertThat(bom).doesNotContain("<name>")
            assertThat(bom).doesNotContain("<description>")
        }
    }

    @Nested
    inner class `Deprecation in BOM` {

        @Test
        fun `deprecated library has XML comment`() = runTest {
            val result = dependanger {
                libraries {
                    library("old-lib", "com.example:old-lib:1.0.0") {
                        deprecated(replacedBy = "new-lib", message = "Use new-lib instead")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig(includeDeprecationComments = true))

            assertThat(bom).contains("<!-- DEPRECATED:")
        }

        @Test
        fun `deprecation comments disabled`() = runTest {
            val result = dependanger {
                libraries {
                    library("old-lib", "com.example:old-lib:1.0.0") {
                        deprecated(replacedBy = "new-lib", message = "Use new-lib instead")
                    }
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig(includeDeprecationComments = false))

            assertThat(bom).doesNotContain("DEPRECATED")
        }
    }

    @Nested
    inner class `Platform libraries in BOM` {

        @Test
        fun `platform library has type pom and scope import`() = runTest {
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
        }
    }

    @Nested
    inner class `Pretty print vs compact` {

        @Test
        fun `pretty print formats XML with indentation`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig(prettyPrint = true))

            assertThat(bom).contains("\n")
            assertThat(bom).containsPattern("\\s{2,}<")
        }

        @Test
        fun `compact format minimizes whitespace`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val prettyBom = result.toBom(bomConfig(prettyPrint = true))
            val compactBom = result.toBom(bomConfig(prettyPrint = false))

            assertThat(compactBom.length).isLessThan(prettyBom.length)
            assertThat(compactBom).contains("<groupId>com.example</groupId>")
        }
    }

    @Nested
    inner class `Distribution-specific BOM` {

        @Test
        fun `BOM from distribution contains only filtered libraries`() = runTest {
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
            val bom = result.toBom(bomConfig())

            assertThat(bom).contains("<groupId>com.android</groupId>")
            assertThat(bom).contains("<groupId>com.common</groupId>")
            assertThat(bom).doesNotContain("<groupId>com.server</groupId>")
        }
    }

    @Nested
    inner class `Multiple libraries ordering` {

        @Test
        fun `libraries appear in BOM`() = runTest {
            val result = dependanger {
                libraries {
                    library("alpha-lib", "com.alpha:lib:1.0.0")
                    library("beta-lib", "com.beta:lib:2.0.0")
                    library("gamma-lib", "com.gamma:lib:3.0.0")
                    library("delta-lib", "com.delta:lib:4.0.0")
                }
            }.process()

            assertThat(result.isSuccess).isTrue()
            val bom = result.toBom(bomConfig())

            assertThat(bom).contains("<groupId>com.alpha</groupId>")
            assertThat(bom).contains("<groupId>com.beta</groupId>")
            assertThat(bom).contains("<groupId>com.gamma</groupId>")
            assertThat(bom).contains("<groupId>com.delta</groupId>")
        }
    }
}
