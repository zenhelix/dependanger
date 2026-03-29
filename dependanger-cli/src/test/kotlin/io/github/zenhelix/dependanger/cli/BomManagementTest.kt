package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.VersionReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BomManagementTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @Nested
    inner class `Adding BOMs` {

        @Test
        fun `adds bom with version from coordinates`() {
            val result = CliTestSupport.runCli(
                "add-bom", "org.springframework.boot:spring-boot-dependencies:3.4.0",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val bom = CliTestSupport.readMetadata(metadataFile).bomImports.find { it.alias == "spring-boot-dependencies" }
            assertThat(bom).isNotNull
            assertThat(bom!!.group).isEqualTo("org.springframework.boot")
            assertThat(bom.artifact).isEqualTo("spring-boot-dependencies")
            assertThat(bom.version).isEqualTo(VersionReference.Literal(version = "3.4.0"))
        }

        @Test
        fun `adds bom with custom alias`() {
            val result = CliTestSupport.runCli(
                "add-bom", "org.springframework.boot:spring-boot-dependencies:3.4.0",
                "--alias", "spring-bom",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(CliTestSupport.readMetadata(metadataFile).bomImports.find { it.alias == "spring-bom" }).isNotNull
        }

        @Test
        fun `adds bom with version ref`() {
            CliTestSupport.runCli("add-version", "spring", "3.4.0", "-i", metadataFile.toString())
            val result = CliTestSupport.runCli(
                "add-bom", "org.springframework.boot:spring-boot-dependencies",
                "-v", "ref:spring",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val bom = CliTestSupport.readMetadata(metadataFile).bomImports.first()
            assertThat(bom.version).isEqualTo(VersionReference.Reference(name = "spring"))
        }

        @Test
        fun `fails without version`() {
            val result = CliTestSupport.runCli(
                "add-bom", "org.springframework.boot:spring-boot-dependencies",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `rejects duplicate bom alias`() {
            CliTestSupport.runCli(
                "add-bom", "org.springframework.boot:spring-boot-dependencies:3.4.0",
                "-i", metadataFile.toString(),
            )
            val result = CliTestSupport.runCli(
                "add-bom", "org.springframework.boot:spring-boot-dependencies:3.5.0",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }
    }

    @Nested
    inner class `Removing BOMs` {

        @Test
        fun `removes existing bom`() {
            CliTestSupport.runCli(
                "add-bom", "org.springframework.boot:spring-boot-dependencies:3.4.0",
                "-i", metadataFile.toString(),
            )
            val result = CliTestSupport.runCli("remove-bom", "spring-boot-dependencies", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(CliTestSupport.readMetadata(metadataFile).bomImports).isEmpty()
        }

        @Test
        fun `fails for nonexistent bom`() {
            val result = CliTestSupport.runCli("remove-bom", "nonexistent", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
