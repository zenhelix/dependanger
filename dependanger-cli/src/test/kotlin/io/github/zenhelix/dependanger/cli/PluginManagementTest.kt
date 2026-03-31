package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.VersionReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PluginManagementTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @Nested
    inner class `Adding plugins` {

        @Test
        fun `adds plugin with literal version`() {
            val result = CliTestSupport.runCli(
                "add", "plugin", "ksp", "com.google.devtools.ksp",
                "-v", "2.1.20-1.0.29",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val plugin = CliTestSupport.readMetadata(metadataFile).plugins.find { it.alias == "ksp" }
            assertThat(plugin).isNotNull
            assertThat(plugin!!.id).isEqualTo("com.google.devtools.ksp")
            assertThat(plugin.version).isEqualTo(VersionReference.Literal(version = "2.1.20-1.0.29"))
        }

        @Test
        fun `adds plugin with version ref`() {
            val result = CliTestSupport.runCli(
                "add", "plugin", "kotlin-serialization", "org.jetbrains.kotlin.plugin.serialization",
                "-v", "ref:kotlin",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val plugin = CliTestSupport.readMetadata(metadataFile).plugins.find { it.alias == "kotlin-serialization" }
            assertThat(plugin!!.version).isEqualTo(VersionReference.Reference(name = "kotlin"))
        }

        @Test
        fun `adds plugin with tags`() {
            val result = CliTestSupport.runCli(
                "add", "plugin", "ksp", "com.google.devtools.ksp",
                "-v", "2.1.20-1.0.29", "-t", "codegen,kotlin",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isEqualTo(0)
            val plugin = CliTestSupport.readMetadata(metadataFile).plugins.find { it.alias == "ksp" }
            assertThat(plugin!!.tags).containsExactlyInAnyOrder("codegen", "kotlin")
        }

        @Test
        fun `rejects duplicate plugin alias`() {
            val pluginsBefore = CliTestSupport.readMetadata(metadataFile).plugins

            val result = CliTestSupport.runCli(
                "add", "plugin", "kotlin-jvm", "org.jetbrains.kotlin.jvm",
                "-v", "2.1.20",
                "-i", metadataFile.toString(),
            )

            assertThat(result.statusCode).isNotEqualTo(0)
            assertThat(CliTestSupport.readMetadata(metadataFile).plugins).isEqualTo(pluginsBefore)
        }
    }

    @Nested
    inner class `Removing plugins` {

        @Test
        fun `removes existing plugin`() {
            val result = CliTestSupport.runCli("remove", "plugin", "kotlin-jvm", "-i", metadataFile.toString())

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(CliTestSupport.readMetadata(metadataFile).plugins).isEmpty()
        }

        @Test
        fun `fails for nonexistent plugin`() {
            val pluginsBefore = CliTestSupport.readMetadata(metadataFile).plugins

            val result = CliTestSupport.runCli("remove", "plugin", "nonexistent", "-i", metadataFile.toString())

            assertThat(result.statusCode).isNotEqualTo(0)
            assertThat(CliTestSupport.readMetadata(metadataFile).plugins).isEqualTo(pluginsBefore)
        }
    }
}
