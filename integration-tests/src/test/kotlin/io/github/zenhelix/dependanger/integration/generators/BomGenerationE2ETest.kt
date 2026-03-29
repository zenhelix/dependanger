package io.github.zenhelix.dependanger.integration.generators

import io.github.zenhelix.dependanger.api.toBom
import io.github.zenhelix.dependanger.api.writeBomTo
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BomGenerationE2ETest : IntegrationTestBase() {

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

    @Test
    fun `generates valid BOM XML with dependencies`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val bom = result.toBom(bomConfig())

        assertThat(bom).contains("<dependencyManagement>")
        assertThat(bom).contains("<dependencies>")
        assertThat(bom).contains("<groupId>org.jetbrains.kotlin</groupId>")
        assertThat(bom).contains("<artifactId>kotlin-stdlib</artifactId>")
        assertThat(bom).contains("<version>2.1.20</version>")
        assertThat(bom).contains("<groupId>io.ktor</groupId>")
        assertThat(bom).contains("<artifactId>ktor-client-core</artifactId>")
        assertThat(bom).contains("<version>3.1.1</version>")
    }

    @Test
    fun `BOM excludes libraries without version`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            libraries {
                library("versioned-lib", "com.example:versioned:1.0.0")
                library("no-version-lib", "com.example:no-version")
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val bom = result.toBom(bomConfig())

        assertThat(bom).contains("<artifactId>versioned</artifactId>")
        // Library without version should not appear as a dependency with version
        assertThat(bom).doesNotContain("<artifactId>no-version</artifactId>")
    }

    @Test
    fun `BOM includes project coordinates`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            libraries {
                library("lib", "com.example:lib:1.0.0")
            }
        }.process()

        assertThat(result.isSuccess).isTrue()
        val bom = result.toBom(
            bomConfig(
                groupId = "com.my.company",
                artifactId = "my-bom",
                version = "2.5.0",
                name = "My BOM",
                description = "My project BOM"
            )
        )

        assertThat(bom).contains("<groupId>com.my.company</groupId>")
        assertThat(bom).contains("<artifactId>my-bom</artifactId>")
        assertThat(bom).contains("<version>2.5.0</version>")
        assertThat(bom).contains("<name>My BOM</name>")
        assertThat(bom).contains("<description>My project BOM</description>")
        assertThat(bom).contains("<packaging>pom</packaging>")
    }

    @Test
    fun `platform libraries get type pom and scope import`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
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

    @Test
    fun `deprecated libraries include XML comment when configured`() = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
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
    fun `writeBomTo writes file to disk`(@TempDir tempDir: Path) = runTest {
        val result = dependanger(ProcessingPreset.DEFAULT) {
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            }
        }.process()

        assertThat(result.isSuccess).isTrue()

        result.writeBomTo(tempDir, bomConfig())

        val outputFile = tempDir.resolve("bom.pom.xml")
        assertThat(outputFile).exists()
        val content = outputFile.toFile().readText()
        assertThat(content).contains("<groupId>org.jetbrains.kotlin</groupId>")
        assertThat(content).contains("<artifactId>kotlin-stdlib</artifactId>")
    }
}
