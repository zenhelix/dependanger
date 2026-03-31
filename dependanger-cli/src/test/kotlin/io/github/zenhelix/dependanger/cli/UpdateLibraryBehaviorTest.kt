package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.VersionReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class UpdateLibraryBehaviorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @Nested
    inner class `Updating version` {

        @Test
        fun `updates to literal version`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib", "-v", "3.0.0",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "stdlib" }
            assertThat(lib).isNotNull
            assertThat(lib!!.version).isEqualTo(VersionReference.Literal(version = "3.0.0"))
        }

        @Test
        fun `updates to version reference`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib", "-v", "ref:coroutines",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "stdlib" }
            assertThat(lib).isNotNull
            assertThat(lib!!.version).isEqualTo(VersionReference.Reference(name = "coroutines"))
        }
    }

    @Nested
    inner class `Updating tags` {

        @Test
        fun `replaces tags`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib", "-t", "new-tag,another",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "stdlib" }
            assertThat(lib).isNotNull
            assertThat(lib!!.tags).isEqualTo(setOf("new-tag", "another"))
        }
    }

    @Nested
    inner class `Updating JDK requirement` {

        @Test
        fun `sets requires-jdk`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib", "--requires-jdk", "21",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "stdlib" }
            assertThat(lib).isNotNull
            assertThat(lib!!.requires).isNotNull
            assertThat(lib.requires!!.jdk).isNotNull
            assertThat(lib.requires!!.jdk!!.min).isEqualTo(21)
        }
    }

    @Nested
    inner class `Combined updates` {

        @Test
        fun `updates version and tags together`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib", "-v", "3.0.0", "-t", "updated",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "stdlib" }
            assertThat(lib).isNotNull
            assertThat(lib!!.version).isEqualTo(VersionReference.Literal(version = "3.0.0"))
            assertThat(lib.tags).isEqualTo(setOf("updated"))
        }
    }

    @Nested
    inner class `No-op update` {

        @Test
        fun `no changes when no options specified`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val lib = CliTestSupport.readMetadata(metadataFile).libraries.find { it.alias == "stdlib" }
            assertThat(lib).isNotNull
            assertThat(lib!!.version).isEqualTo(VersionReference.Reference(name = "kotlin"))
            assertThat(lib.tags).isEqualTo(setOf("kotlin", "core"))
        }
    }

    @Nested
    inner class `Error handling` {

        @Test
        fun `fails for nonexistent library`() {
            val result = CliTestSupport.runCli(
                "update", "library", "nonexistent", "-v", "1.0",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `fails when input file not found`() {
            val result = CliTestSupport.runCli(
                "update", "library", "stdlib", "-v", "1.0",
                "-i", tempDir.resolve("nonexistent.json").toString(),
            )

            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
