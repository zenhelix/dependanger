package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.VersionReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class VersionManagementTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @Nested
    inner class `Adding versions` {

        @Test
        fun `adds a new version`() {
            val result = CliTestSupport.runCli("add", "version", "ktor", "3.1.1", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            val ktor = metadata.versions.find { it.name == "ktor" }
            assertThat(ktor).isNotNull
            assertThat(ktor!!.value).isEqualTo("3.1.1")
        }

        @Test
        fun `rejects duplicate version alias`() {
            val result = CliTestSupport.runCli("add", "version", "kotlin", "2.2.0", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(1)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions.filter { it.name == "kotlin" }).hasSize(1)
            assertThat(metadata.versions.find { it.name == "kotlin" }!!.value).isEqualTo("2.1.20")
        }
    }

    @Nested
    inner class `Removing versions` {

        @Test
        fun `removes an unreferenced version`() {
            CliTestSupport.runCli("add", "version", "ktor", "3.1.1", "-i", metadataFile.toString())

            val result = CliTestSupport.runCli("remove", "version", "ktor", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions.none { it.name == "ktor" }).isTrue()
        }

        @Test
        fun `fails to remove version referenced by library`() {
            val result = CliTestSupport.runCli("remove", "version", "kotlin", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(1)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions.find { it.name == "kotlin" }).isNotNull
        }

        @Test
        fun `force removes version despite references`() {
            val result = CliTestSupport.runCli("remove", "version", "kotlin", "--force", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions.none { it.name == "kotlin" }).isTrue()
        }

        @Test
        fun `fails when version does not exist`() {
            val result = CliTestSupport.runCli("remove", "version", "nonexistent", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(1)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions).hasSize(2)
        }
    }

    @Nested
    inner class `Updating versions` {

        @Test
        fun `updates version value`() {
            val result = CliTestSupport.runCli("update", "version", "kotlin", "2.2.0", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions.find { it.name == "kotlin" }!!.value).isEqualTo("2.2.0")
        }

        @Test
        fun `updates library version directly`() {
            val result = CliTestSupport.runCli("update", "version", "stdlib", "2.2.0", "--library", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            val lib = metadata.libraries.find { it.alias == "stdlib" }!!
            assertThat(lib.version).isEqualTo(VersionReference.Literal(version = "2.2.0"))
        }

        @Test
        fun `fails when version alias does not exist`() {
            val result = CliTestSupport.runCli("update", "version", "nonexistent", "1.0", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(1)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions.map { it.value }).doesNotContain("1.0")
        }
    }
}
