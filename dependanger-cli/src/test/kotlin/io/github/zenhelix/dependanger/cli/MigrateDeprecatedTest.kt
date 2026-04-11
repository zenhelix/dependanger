package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.DeprecationInfo
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MigrateDeprecatedTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    private fun metadataWithDeprecated() = CliTestSupport.minimalMetadata().let { base ->
        base.copy(
            libraries = base.libraries.map { lib ->
                if (lib.alias == "stdlib") {
                    lib.copy(
                        deprecation = DeprecationInfo(
                            replacedBy = "stdlib-jdk8",
                            message = "Use stdlib-jdk8 instead",
                            since = null,
                            removalVersion = null,
                        ),
                    )
                } else {
                    lib
                }
            } + base.libraries.first().let { first ->
                first.copy(
                    alias = "stdlib-jdk8",
                    coordinate = MavenCoordinate(first.coordinate.group, "kotlin-stdlib-jdk8"),
                    deprecation = null,
                )
            },
        )
    }

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, metadataWithDeprecated())
    }

    @Nested
    inner class `Dry run` {

        @Test
        fun `shows migration plan without modifying file`() {
            val originalContent = metadataFile.toFile().readText()
            val result = CliTestSupport.runCli("migrate-deprecated", "--dry-run", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(metadataFile.toFile().readText()).isEqualTo(originalContent)
        }
    }

    @Nested
    inner class `Migration execution` {

        @Test
        fun `replaces deprecated in bundles`() {
            val result = CliTestSupport.runCli("migrate-deprecated", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            val bundle = metadata.bundles.find { it.alias == "kotlin-essentials" }!!
            assertThat(bundle.libraries).contains("stdlib-jdk8")
            assertThat(bundle.libraries).doesNotContain("stdlib")
        }

        @Test
        fun `removes deprecated libraries when requested`() {
            val result = CliTestSupport.runCli("migrate-deprecated", "--remove", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.libraries.none { it.deprecation != null }).isTrue()
        }

        @Test
        fun `creates backup when requested`() {
            val result = CliTestSupport.runCli("migrate-deprecated", "--backup", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(Path.of("${metadataFile}.bak")).exists()
        }
    }

    @Nested
    inner class `No deprecated libraries` {

        @Test
        fun `succeeds with no changes when nothing deprecated`() {
            val cleanDir = tempDir.resolve("clean")
            cleanDir.toFile().mkdirs()
            val cleanMeta = CliTestSupport.writeMetadata(cleanDir, CliTestSupport.minimalMetadata())
            val result = CliTestSupport.runCli("migrate-deprecated", "-i", cleanMeta.toString())
            assertThat(result.statusCode).isEqualTo(0)
        }
    }
}
