package io.github.zenhelix.dependanger.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class GenerateBehaviorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path
    private lateinit var outputDir: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
        outputDir = tempDir.resolve("output")
        outputDir.toFile().mkdirs()
    }

    @Nested
    inner class `TOML generation` {

        @Test
        fun `generates toml version catalog`() {
            val result = CliTestSupport.runCli(
                "generate", "--toml", "-i", metadataFile.toString(), "-o", outputDir.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val tomlFile = outputDir.resolve(CliDefaults.TOML_FILENAME)
            assertThat(tomlFile).exists()
            val content = tomlFile.readText()
            assertThat(content).contains("[versions]")
            assertThat(content).contains("[libraries]")
            assertThat(content).contains("kotlin")
        }

        @Test
        fun `toml contains declared versions`() {
            CliTestSupport.runCli("generate", "--toml", "-i", metadataFile.toString(), "-o", outputDir.toString())
            val content = outputDir.resolve(CliDefaults.TOML_FILENAME).readText()
            assertThat(content).contains("2.1.20")
            assertThat(content).contains("1.10.1")
        }

        @Test
        fun `toml contains library entries`() {
            CliTestSupport.runCli("generate", "--toml", "-i", metadataFile.toString(), "-o", outputDir.toString())
            val content = outputDir.resolve(CliDefaults.TOML_FILENAME).readText()
            assertThat(content).contains("org.jetbrains.kotlin")
            assertThat(content).contains("kotlin-stdlib")
        }

        @Test
        fun `custom toml filename`() {
            val result = CliTestSupport.runCli(
                "generate", "--toml", "--toml-filename", "custom.versions.toml",
                "-i", metadataFile.toString(), "-o", outputDir.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(outputDir.resolve("custom.versions.toml")).exists()
        }
    }

    @Nested
    inner class `BOM generation` {

        @Test
        fun `generates maven bom`() {
            val result = CliTestSupport.runCli(
                "generate", "--bom",
                "--bom-group", "com.example", "--bom-artifact", "test-bom", "--bom-version", "1.0.0",
                "-i", metadataFile.toString(), "-o", outputDir.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val bomFile = outputDir.resolve("bom.pom.xml")
            assertThat(bomFile).exists()
            val content = bomFile.readText()
            assertThat(content).contains("com.example")
            assertThat(content).contains("test-bom")
            assertThat(content).contains("1.0.0")
            assertThat(content).contains("dependencyManagement")
        }

        @Test
        fun `bom generation fails without required args`() {
            val result = CliTestSupport.runCli(
                "generate", "--bom", "-i", metadataFile.toString(), "-o", outputDir.toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }
    }

    @Nested
    inner class `Combined generation` {

        @Test
        fun `generates both toml and bom`() {
            val result = CliTestSupport.runCli(
                "generate", "--toml", "--bom",
                "--bom-group", "com.example", "--bom-artifact", "test-bom", "--bom-version", "1.0.0",
                "-i", metadataFile.toString(), "-o", outputDir.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(outputDir.resolve(CliDefaults.TOML_FILENAME)).exists()
            assertThat(outputDir.resolve("bom.pom.xml")).exists()
        }
    }
}
