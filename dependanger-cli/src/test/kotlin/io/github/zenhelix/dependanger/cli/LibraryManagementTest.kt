package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.VersionReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LibraryManagementTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @Nested
    inner class `Adding libraries` {

        @Test
        fun `adds library with inline version from coordinates`() {
            val result = CliTestSupport.runCli(
                "add", "library", "ktor-core", "io.ktor:ktor-client-core:3.1.1",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "ktor-core" }
            assertThat(lib).isNotNull
            assertThat(lib!!.group).isEqualTo("io.ktor")
            assertThat(lib.artifact).isEqualTo("ktor-client-core")
            assertThat(lib.version).isEqualTo(VersionReference.Literal(version = "3.1.1"))
        }

        @Test
        fun `adds library with version ref`() {
            val result = CliTestSupport.runCli(
                "add", "library", "reflect", "org.jetbrains.kotlin:kotlin-reflect",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "reflect" }
            assertThat(lib!!.version).isEqualTo(VersionReference.Reference(name = "kotlin"))
        }

        @Test
        fun `adds library with explicit version flag`() {
            val result = CliTestSupport.runCli(
                "add", "library", "ktor-core", "io.ktor:ktor-client-core",
                "-v", "3.1.1",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "ktor-core" }
            assertThat(lib!!.version).isEqualTo(VersionReference.Literal(version = "3.1.1"))
        }

        @Test
        fun `adds library with tags`() {
            val result = CliTestSupport.runCli(
                "add", "library", "ktor-core", "io.ktor:ktor-client-core:3.1.1",
                "-t", "networking,ktor",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "ktor-core" }
            assertThat(lib!!.tags).containsExactlyInAnyOrder("networking", "ktor")
        }

        @Test
        fun `rejects duplicate library alias`() {
            val result = CliTestSupport.runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isNotEqualTo(0)
        }
    }

    @Nested
    inner class `Removing libraries` {

        @Test
        fun `removes an unreferenced library`() {
            CliTestSupport.runCli(
                "add", "library", "ktor-core", "io.ktor:ktor-client-core:3.1.1",
                "-i", metadataFile.toString(),
            )

            val result = CliTestSupport.runCli("remove", "library", "ktor-core", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(CliTestSupport.readMetadata(metadataFile).libraries.none { it.alias == "ktor-core" }).isTrue()
        }

        @Test
        fun `fails to remove library referenced by bundle`() {
            val result = CliTestSupport.runCli("remove", "library", "stdlib", "-i", metadataFile.toString())

            assertThat(result.statusCode).isNotEqualTo(0)
        }

        @Test
        fun `force removes library despite bundle reference`() {
            val result = CliTestSupport.runCli("remove", "library", "stdlib", "--force", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(CliTestSupport.readMetadata(metadataFile).libraries.none { it.alias == "stdlib" }).isTrue()
        }
    }

    @Nested
    inner class `Updating libraries` {

        @Test
        fun `updates library version`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib", "-v", "2.2.0",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "stdlib" }
            assertThat(lib!!.version).isEqualTo(VersionReference.Literal(version = "2.2.0"))
        }

        @Test
        fun `updates library version to ref`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib", "-v", "ref:coroutines",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "stdlib" }
            assertThat(lib!!.version).isEqualTo(VersionReference.Reference(name = "coroutines"))
        }

        @Test
        fun `updates library tags`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib", "-t", "new-tag,updated",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "stdlib" }
            assertThat(lib!!.tags).containsExactlyInAnyOrder("new-tag", "updated")
        }

        @Test
        fun `fails for nonexistent library`() {
            val result = CliTestSupport.runCli(
                "update", "library", "nonexistent", "-v", "1.0",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isNotEqualTo(0)
        }
    }
}
