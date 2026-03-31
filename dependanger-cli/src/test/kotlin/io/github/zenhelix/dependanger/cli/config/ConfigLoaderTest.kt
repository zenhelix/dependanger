package io.github.zenhelix.dependanger.cli.config

import io.github.zenhelix.dependanger.cli.CliException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.writeText

class ConfigLoaderTest {

    private val loader = ConfigLoader()

    @Nested
    inner class `load with explicit path` {

        @Test
        fun `parses valid YAML config with all fields`(@TempDir tempDir: Path) {
            val configFile = tempDir.resolve(".dependanger.yaml")
            configFile.writeText(
                """
                defaults:
                  input: ./custom-input.json
                  output: ./custom-output
                generate:
                  toml:
                    filename: custom.versions.toml
                  bom:
                    group-id: com.example
                    artifact-id: my-bom
                check-updates:
                  include-pre-release: true
                  fail-on-major: true
                analyze:
                  target-jdk: 21
                  fail-on-error: true
                security-check:
                  min-severity: critical
                  fail-on: critical
                license-check:
                  fail-on-denied: true
                  fail-on-unknown: true
                """.trimIndent()
            )

            val config = loader.load(configFile)

            assertThat(config.defaults.input).isEqualTo("./custom-input.json")
            assertThat(config.defaults.output).isEqualTo("./custom-output")
            assertThat(config.generate.toml.filename).isEqualTo("custom.versions.toml")
            assertThat(config.generate.bom.groupId).isEqualTo("com.example")
            assertThat(config.generate.bom.artifactId).isEqualTo("my-bom")
            assertThat(config.checkUpdates.includePreRelease).isTrue()
            assertThat(config.checkUpdates.failOnMajor).isTrue()
            assertThat(config.analyze.targetJdk).isEqualTo(21)
            assertThat(config.analyze.failOnError).isTrue()
            assertThat(config.securityCheck.minSeverity).isEqualTo("critical")
            assertThat(config.securityCheck.failOn).isEqualTo("critical")
            assertThat(config.licenseCheck.failOnDenied).isTrue()
            assertThat(config.licenseCheck.failOnUnknown).isTrue()
        }

        @Test
        fun `parses partial YAML using defaults for missing fields`(@TempDir tempDir: Path) {
            val configFile = tempDir.resolve(".dependanger.yaml")
            configFile.writeText(
                """
                defaults:
                  input: ./my-metadata.json
                """.trimIndent()
            )

            val config = loader.load(configFile)

            assertThat(config.defaults.input).isEqualTo("./my-metadata.json")
            assertThat(config.defaults.output).isEqualTo("./build/dependanger")
            assertThat(config.generate.toml.filename).isEqualTo("libs.versions.toml")
            assertThat(config.generate.bom.groupId).isNull()
            assertThat(config.generate.bom.artifactId).isNull()
            assertThat(config.checkUpdates.includePreRelease).isFalse()
            assertThat(config.analyze.targetJdk).isEqualTo(17)
        }

        @Test
        fun `parses empty YAML as default config`(@TempDir tempDir: Path) {
            val configFile = tempDir.resolve(".dependanger.yaml")
            configFile.writeText("{}")

            val config = loader.load(configFile)

            assertThat(config).isEqualTo(CliConfig())
        }

        @Test
        fun `throws ParseError for invalid YAML`(@TempDir tempDir: Path) {
            val configFile = tempDir.resolve(".dependanger.yaml")
            configFile.writeText("defaults: [invalid: yaml: content")

            assertThatThrownBy { loader.load(configFile) }
                .isInstanceOf(CliException.ParseError::class.java)
                .hasMessageContaining("Failed to parse config file")
        }

        @Test
        fun `throws ParseError for YAML with unknown fields`(@TempDir tempDir: Path) {
            val configFile = tempDir.resolve(".dependanger.yaml")
            configFile.writeText("unknown-top-level-key: value")

            assertThatThrownBy { loader.load(configFile) }
                .isInstanceOf(CliException.ParseError::class.java)
        }

        @Test
        fun `throws when path points to non-existent file`(@TempDir tempDir: Path) {
            val missingFile = tempDir.resolve("does-not-exist.yaml")

            assertThatThrownBy { loader.load(missingFile) }
                .isInstanceOf(NoSuchFileException::class.java)
        }
    }

    @Nested
    inner class `load without path` {

        @Test
        fun `returns default CliConfig when no config file found`() {
            val originalHome = System.getProperty("user.home")
            try {
                // Ensure home directory check also fails
                System.setProperty("user.home", "/nonexistent-path-for-test")

                val config = loader.load()

                assertThat(config).isEqualTo(CliConfig())
            } finally {
                System.setProperty("user.home", originalHome)
            }
        }
    }

    @Nested
    inner class `findConfigFile` {

        @Test
        fun `throws FileNotFound when no config exists anywhere`() {
            val originalHome = System.getProperty("user.home")
            try {
                System.setProperty("user.home", "/nonexistent-path-for-test")

                assertThatThrownBy { loader.findConfigFile() }
                    .isInstanceOf(CliException.FileNotFound::class.java)
                    .hasMessageContaining(ConfigLoader.DEFAULT_CONFIG_FILENAME)
            } finally {
                System.setProperty("user.home", originalHome)
            }
        }

        @Test
        fun `finds config in home directory`(@TempDir tempDir: Path) {
            val configFile = tempDir.resolve(ConfigLoader.DEFAULT_CONFIG_FILENAME)
            configFile.writeText("defaults:\n  input: ./test.json")

            val originalHome = System.getProperty("user.home")
            try {
                System.setProperty("user.home", tempDir.toString())

                val found = loader.findConfigFile()

                assertThat(found).isEqualTo(configFile)
            } finally {
                System.setProperty("user.home", originalHome)
            }
        }
    }
}
