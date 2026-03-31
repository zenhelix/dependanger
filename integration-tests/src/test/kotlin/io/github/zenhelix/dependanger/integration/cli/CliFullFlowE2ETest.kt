package io.github.zenhelix.dependanger.integration.cli

import com.github.ajalt.clikt.testing.CliktCommandTestResult
import io.github.zenhelix.dependanger.cli.CliTestSupport
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

@DisplayName("CLI Full Flow E2E")
class CliFullFlowE2ETest {

    @TempDir
    lateinit var tempDir: Path

    private fun runCli(vararg args: String): CliktCommandTestResult =
        CliTestSupport.runCli(*args)

    private val metadataFile: Path get() = tempDir.resolve("metadata.json")

    private fun initMetadata(): CliktCommandTestResult =
        runCli("init", "-o", metadataFile.toString())

    @Nested
    @DisplayName("Full Metadata Lifecycle")
    inner class FullMetadataLifecycle {

        @Test
        fun `init then add versions libraries plugins bundles then validate`() {
            val init = initMetadata()
            assertThat(init.statusCode).isEqualTo(0)
            assertThat(metadataFile).exists()

            val addVersion = runCli(
                "add", "version", "kotlin", "2.1.20",
                "-i", metadataFile.toString()
            )
            assertThat(addVersion.statusCode).isEqualTo(0)

            val addLibrary = runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )
            assertThat(addLibrary.statusCode).isEqualTo(0)

            val addPlugin = runCli(
                "add", "plugin", "kotlin-jvm", "org.jetbrains.kotlin.jvm",
                "-v", "ref:kotlin",
                "-i", metadataFile.toString()
            )
            assertThat(addPlugin.statusCode).isEqualTo(0)

            val addBundle = runCli(
                "add", "bundle", "kotlin-essentials",
                "--libraries", "stdlib",
                "-i", metadataFile.toString()
            )
            assertThat(addBundle.statusCode).isEqualTo(0)

            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions).hasSize(1)
            assertThat(metadata.versions.first().name).isEqualTo("kotlin")
            assertThat(metadata.versions.first().value).isEqualTo("2.1.20")
            assertThat(metadata.libraries).hasSize(1)
            assertThat(metadata.libraries.first().alias).isEqualTo("stdlib")
            assertThat(metadata.plugins).hasSize(1)
            assertThat(metadata.plugins.first().alias).isEqualTo("kotlin-jvm")
            assertThat(metadata.bundles).hasSize(1)
            assertThat(metadata.bundles.first().alias).isEqualTo("kotlin-essentials")
        }

        @Test
        fun `init with force overwrites existing file`() {
            initMetadata()
            assertThat(metadataFile).exists()

            runCli(
                "add", "version", "kotlin", "2.1.20",
                "-i", metadataFile.toString()
            )
            val metadataBefore = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataBefore.versions).hasSize(1)

            val reinit = runCli("init", "-o", metadataFile.toString(), "--force")
            assertThat(reinit.statusCode).isEqualTo(0)

            val metadataAfter = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfter.versions).isEmpty()
        }

        @Test
        fun `init without force fails on existing file`() {
            initMetadata()

            val reinit = runCli("init", "-o", metadataFile.toString())
            assertThat(reinit.statusCode).isNotEqualTo(0)
        }

        @Test
        fun `build up metadata then generate TOML`() {
            initMetadata()

            runCli(
                "add", "version", "kotlin", "2.1.20",
                "-i", metadataFile.toString()
            )
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )
            runCli(
                "add", "plugin", "kotlin-jvm", "org.jetbrains.kotlin.jvm",
                "-v", "ref:kotlin",
                "-i", metadataFile.toString()
            )

            val outputDir = tempDir.resolve("output")
            outputDir.toFile().mkdirs()

            val generate = runCli(
                "generate",
                "-i", metadataFile.toString(),
                "-o", outputDir.toString(),
                "--toml"
            )
            assertThat(generate.statusCode).isEqualTo(0)

            val tomlFile = outputDir.resolve("libs.versions.toml")
            assertThat(tomlFile).exists()
            val tomlContent = tomlFile.readText()
            assertThat(tomlContent).contains("kotlin")
            assertThat(tomlContent).contains("2.1.20")
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    inner class CrudOperations {

        @Test
        fun `add and remove version`() {
            initMetadata()

            val add = runCli(
                "add", "version", "kotlin", "2.1.20",
                "-i", metadataFile.toString()
            )
            assertThat(add.statusCode).isEqualTo(0)

            val metadataAfterAdd = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterAdd.versions).hasSize(1)

            val remove = runCli(
                "remove", "version", "kotlin",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isEqualTo(0)

            val metadataAfterRemove = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterRemove.versions).isEmpty()
        }

        @Test
        fun `add and remove library`() {
            initMetadata()

            val add = runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "-v", "2.1.20",
                "-i", metadataFile.toString()
            )
            assertThat(add.statusCode).isEqualTo(0)

            val metadataAfterAdd = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterAdd.libraries).hasSize(1)

            val remove = runCli(
                "remove", "library", "stdlib",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isEqualTo(0)

            val metadataAfterRemove = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterRemove.libraries).isEmpty()
        }

        @Test
        fun `add and remove plugin`() {
            initMetadata()

            val add = runCli(
                "add", "plugin", "kotlin-jvm", "org.jetbrains.kotlin.jvm",
                "-v", "2.1.20",
                "-i", metadataFile.toString()
            )
            assertThat(add.statusCode).isEqualTo(0)

            val metadataAfterAdd = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterAdd.plugins).hasSize(1)

            val remove = runCli(
                "remove", "plugin", "kotlin-jvm",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isEqualTo(0)

            val metadataAfterRemove = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterRemove.plugins).isEmpty()
        }

        @Test
        fun `add and remove bundle`() {
            initMetadata()

            val add = runCli(
                "add", "bundle", "my-bundle",
                "--libraries", "lib-a,lib-b",
                "-i", metadataFile.toString()
            )
            assertThat(add.statusCode).isEqualTo(0)

            val metadataAfterAdd = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterAdd.bundles).hasSize(1)
            assertThat(metadataAfterAdd.bundles.first().libraries).containsExactly("lib-a", "lib-b")

            val remove = runCli(
                "remove", "bundle", "my-bundle",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isEqualTo(0)

            val metadataAfterRemove = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterRemove.bundles).isEmpty()
        }

        @Test
        fun `add and remove bom import`() {
            initMetadata()

            val add = runCli(
                "add", "bom", "org.springframework.boot:spring-boot-dependencies:3.4.3",
                "--alias", "spring-boot-bom",
                "-i", metadataFile.toString()
            )
            assertThat(add.statusCode).isEqualTo(0)

            val metadataAfterAdd = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterAdd.bomImports).hasSize(1)
            assertThat(metadataAfterAdd.bomImports.first().alias).isEqualTo("spring-boot-bom")

            val remove = runCli(
                "remove", "bom", "spring-boot-bom",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isEqualTo(0)

            val metadataAfterRemove = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterRemove.bomImports).isEmpty()
        }

        @Test
        fun `add and remove distribution`() {
            initMetadata()

            val add = runCli(
                "add", "distribution", "server",
                "--include-bundles", "kotlin-essentials,ktor-server",
                "-i", metadataFile.toString()
            )
            assertThat(add.statusCode).isEqualTo(0)

            val metadataAfterAdd = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterAdd.distributions).hasSize(1)
            assertThat(metadataAfterAdd.distributions.first().name).isEqualTo("server")

            val remove = runCli(
                "remove", "distribution", "server",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isEqualTo(0)

            val metadataAfterRemove = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfterRemove.distributions).isEmpty()
        }

        @Test
        fun `update version value`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.0.0", "-i", metadataFile.toString())

            val update = runCli(
                "update", "version", "kotlin", "2.1.20",
                "-i", metadataFile.toString()
            )
            assertThat(update.statusCode).isEqualTo(0)

            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions.first().value).isEqualTo("2.1.20")
        }

        @Test
        fun `update library version via update-version with library flag`() {
            initMetadata()
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "-v", "2.0.0",
                "-i", metadataFile.toString()
            )

            val update = runCli(
                "update", "version", "stdlib", "2.1.20",
                "--library",
                "-i", metadataFile.toString()
            )
            assertThat(update.statusCode).isEqualTo(0)

            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.libraries.first().alias).isEqualTo("stdlib")
        }

        @Test
        fun `update library tags and version via update-library`() {
            initMetadata()
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "-v", "2.0.0",
                "-i", metadataFile.toString()
            )

            val update = runCli(
                "update", "library", "stdlib",
                "-v", "2.1.20",
                "-t", "kotlin,core",
                "-i", metadataFile.toString()
            )
            assertThat(update.statusCode).isEqualTo(0)

            val metadata = CliTestSupport.readMetadata(metadataFile)
            val lib = metadata.libraries.first()
            assertThat(lib.tags).containsExactlyInAnyOrder("kotlin", "core")
        }

        @Test
        fun `add library with inline version from coordinates`() {
            initMetadata()

            val add = runCli(
                "add", "library", "assertj", "org.assertj:assertj-core:3.27.3",
                "-i", metadataFile.toString()
            )
            assertThat(add.statusCode).isEqualTo(0)

            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.libraries.first().alias).isEqualTo("assertj")
        }

        @Test
        fun `add library with tags`() {
            initMetadata()

            val add = runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "-v", "2.1.20",
                "-t", "kotlin,core,essential",
                "-i", metadataFile.toString()
            )
            assertThat(add.statusCode).isEqualTo(0)

            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.libraries.first().tags).containsExactlyInAnyOrder("kotlin", "core", "essential")
        }
    }

    @Nested
    @DisplayName("Duplicate and Not Found Errors")
    inner class DuplicateAndNotFoundErrors {

        @Test
        fun `add duplicate version returns error`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())

            val duplicate = runCli("add", "version", "kotlin", "2.2.0", "-i", metadataFile.toString())
            assertThat(duplicate.statusCode).isNotEqualTo(0)
            assertThat(duplicate.output).contains("already exists")
        }

        @Test
        fun `add duplicate library returns error`() {
            initMetadata()
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "-v", "2.1.20",
                "-i", metadataFile.toString()
            )

            val duplicate = runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "-v", "2.2.0",
                "-i", metadataFile.toString()
            )
            assertThat(duplicate.statusCode).isNotEqualTo(0)
            assertThat(duplicate.output).contains("already exists")
        }

        @Test
        fun `add duplicate plugin returns error`() {
            initMetadata()
            runCli(
                "add", "plugin", "kotlin-jvm", "org.jetbrains.kotlin.jvm",
                "-v", "2.1.20",
                "-i", metadataFile.toString()
            )

            val duplicate = runCli(
                "add", "plugin", "kotlin-jvm", "org.jetbrains.kotlin.jvm",
                "-v", "2.2.0",
                "-i", metadataFile.toString()
            )
            assertThat(duplicate.statusCode).isNotEqualTo(0)
            assertThat(duplicate.output).contains("already exists")
        }

        @Test
        fun `remove nonexistent version returns error`() {
            initMetadata()

            val remove = runCli(
                "remove", "version", "nonexistent",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isNotEqualTo(0)
            assertThat(remove.output).contains("not found")
        }

        @Test
        fun `remove nonexistent library returns error`() {
            initMetadata()

            val remove = runCli(
                "remove", "library", "nonexistent",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isNotEqualTo(0)
            assertThat(remove.output).contains("not found")
        }

        @Test
        fun `remove nonexistent plugin returns error`() {
            initMetadata()

            val remove = runCli(
                "remove", "plugin", "nonexistent",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isNotEqualTo(0)
            assertThat(remove.output).contains("not found")
        }

        @Test
        fun `remove nonexistent bundle returns error`() {
            initMetadata()

            val remove = runCli(
                "remove", "bundle", "nonexistent",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isNotEqualTo(0)
            assertThat(remove.output).contains("not found")
        }

        @Test
        fun `update nonexistent version returns error`() {
            initMetadata()

            val update = runCli(
                "update", "version", "nonexistent", "1.0.0",
                "-i", metadataFile.toString()
            )
            assertThat(update.statusCode).isNotEqualTo(0)
            assertThat(update.output).contains("not found")
        }

        @Test
        fun `update nonexistent library returns error`() {
            initMetadata()

            val update = runCli(
                "update", "library", "nonexistent",
                "-v", "1.0.0",
                "-i", metadataFile.toString()
            )
            assertThat(update.statusCode).isNotEqualTo(0)
            assertThat(update.output).contains("not found")
        }
    }

    @Nested
    @DisplayName("Reference Conflict Checks")
    inner class ReferenceConflictChecks {

        @Test
        fun `remove version referenced by library fails without force`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            val remove = runCli(
                "remove", "version", "kotlin",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isNotEqualTo(0)
            assertThat(remove.output).contains("referenced by")
        }

        @Test
        fun `remove version referenced by library succeeds with force`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            val remove = runCli(
                "remove", "version", "kotlin",
                "--force",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isEqualTo(0)

            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions).isEmpty()
            assertThat(metadata.libraries).hasSize(1)
        }

        @Test
        fun `remove library referenced by bundle fails without force`() {
            initMetadata()
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "-v", "2.1.20",
                "-i", metadataFile.toString()
            )
            runCli(
                "add", "bundle", "essentials",
                "--libraries", "stdlib",
                "-i", metadataFile.toString()
            )

            val remove = runCli(
                "remove", "library", "stdlib",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isNotEqualTo(0)
            assertThat(remove.output).contains("referenced by")
        }

        @Test
        fun `remove library referenced by bundle succeeds with force`() {
            initMetadata()
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "-v", "2.1.20",
                "-i", metadataFile.toString()
            )
            runCli(
                "add", "bundle", "essentials",
                "--libraries", "stdlib",
                "-i", metadataFile.toString()
            )

            val remove = runCli(
                "remove", "library", "stdlib",
                "--force",
                "-i", metadataFile.toString()
            )
            assertThat(remove.statusCode).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        fun `command on missing metadata file returns error`() {
            val nonexistentFile = tempDir.resolve("nonexistent.json")
            val result = runCli(
                "add", "version", "kotlin", "2.1.20",
                "-i", nonexistentFile.toString()
            )
            assertThat(result.statusCode).isNotEqualTo(0)
        }

        @Test
        fun `add-library with invalid coordinates returns error`() {
            initMetadata()

            val result = runCli(
                "add", "library", "bad", "invalid-coordinates",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isNotEqualTo(0)
            assertThat(result.output).contains("Invalid Maven coordinates")
        }

        @Test
        fun `add-library with too many coordinate parts returns error`() {
            initMetadata()

            val result = runCli(
                "add", "library", "bad", "a:b:c:d",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isNotEqualTo(0)
            assertThat(result.output).contains("Invalid Maven coordinates")
        }

        @Test
        fun `add-bom without version returns error`() {
            initMetadata()

            val result = runCli(
                "add", "bom", "org.springframework.boot:spring-boot-dependencies",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isNotEqualTo(0)
            assertThat(result.output).contains("version is required")
        }
    }

    @Nested
    @DisplayName("Migrate Deprecated")
    inner class MigrateDeprecatedTests {

        @Test
        fun `migrate-deprecated dry-run shows plan without modifying file`() {
            initMetadata()
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "-v", "2.1.20",
                "-i", metadataFile.toString()
            )
            // A non-deprecated library - dry-run should report no deprecated found
            val metadataBefore = CliTestSupport.readMetadata(metadataFile)

            val result = runCli(
                "migrate-deprecated",
                "--dry-run",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("No deprecated libraries found")

            val metadataAfter = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadataAfter).isEqualTo(metadataBefore)
        }
    }

    @Nested
    @DisplayName("Generation Flow")
    inner class GenerationFlow {

        @Test
        fun `generate TOML from metadata with versions and libraries`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            val outputDir = tempDir.resolve("toml-output")
            outputDir.toFile().mkdirs()

            val result = runCli(
                "generate",
                "-i", metadataFile.toString(),
                "-o", outputDir.toString(),
                "--toml"
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("Generated TOML")

            val tomlFile = outputDir.resolve("libs.versions.toml")
            assertThat(tomlFile).exists()
            val toml = tomlFile.readText()
            assertThat(toml).contains("kotlin")
            assertThat(toml).contains("2.1.20")
            assertThat(toml).contains("stdlib")
        }

        @Test
        fun `generate TOML with custom filename`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            val outputDir = tempDir.resolve("custom-output")
            outputDir.toFile().mkdirs()

            val result = runCli(
                "generate",
                "-i", metadataFile.toString(),
                "-o", outputDir.toString(),
                "--toml",
                "--toml-filename", "custom.versions.toml"
            )
            assertThat(result.statusCode).isEqualTo(0)

            val customToml = outputDir.resolve("custom.versions.toml")
            assertThat(customToml).exists()
        }

        @Test
        fun `generate BOM from metadata`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            val outputDir = tempDir.resolve("bom-output")
            outputDir.toFile().mkdirs()

            val result = runCli(
                "generate",
                "-i", metadataFile.toString(),
                "-o", outputDir.toString(),
                "--bom",
                "--bom-group", "io.test",
                "--bom-artifact", "test-bom",
                "--bom-version", "1.0.0"
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("Generated Maven BOM")
        }

        @Test
        fun `generate both TOML and BOM`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            val outputDir = tempDir.resolve("both-output")
            outputDir.toFile().mkdirs()

            val result = runCli(
                "generate",
                "-i", metadataFile.toString(),
                "-o", outputDir.toString(),
                "--toml",
                "--bom",
                "--bom-group", "io.test",
                "--bom-artifact", "test-bom",
                "--bom-version", "1.0.0"
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("Generated TOML")
            assertThat(result.output).contains("Generated Maven BOM")
        }

        @Test
        fun `generate BOM without required group fails`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            val outputDir = tempDir.resolve("fail-output")
            outputDir.toFile().mkdirs()

            val result = runCli(
                "generate",
                "-i", metadataFile.toString(),
                "-o", outputDir.toString(),
                "--bom",
                "--bom-artifact", "test-bom",
                "--bom-version", "1.0.0"
            )
            assertThat(result.statusCode).isNotEqualTo(0)
            assertThat(result.output).contains("--bom-group is required")
        }
    }

    @Nested
    @DisplayName("Process Command")
    inner class ProcessCommandTests {

        @Test
        fun `process writes effective metadata to output file`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            val effectiveFile = tempDir.resolve("effective.json")

            val result = runCli(
                "process",
                "-i", metadataFile.toString(),
                "-o", effectiveFile.toString()
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(effectiveFile).exists()
            assertThat(effectiveFile.readText()).contains("kotlin")
        }

        @Test
        fun `process with invalid preset returns error`() {
            initMetadata()

            val result = runCli(
                "process",
                "-i", metadataFile.toString(),
                "--preset", "NONEXISTENT"
            )
            assertThat(result.statusCode).isNotEqualTo(0)
            assertThat(result.output).contains("Unknown preset")
        }
    }

    @Nested
    @DisplayName("Feature Commands with Mocks")
    inner class FeatureCommandsWithMocks {

        private var dependangerMockCleanup: AutoCloseable? = null

        @AfterEach
        fun tearDown() {
            dependangerMockCleanup?.close()
        }

        private fun mockDependangerProcessing(
            extensions: Map<ExtensionKey<*>, Any> = emptyMap(),
        ): AutoCloseable = CliTestSupport.mockDependangerResult(extensions = extensions)

        @Test
        fun `validate valid metadata returns success`() {
            initMetadata()
            dependangerMockCleanup = mockDependangerProcessing()

            val result = runCli(
                "validate",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("Validation passed")
        }

        @Test
        fun `check-updates with no updates available`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            dependangerMockCleanup = mockDependangerProcessing()

            val result = runCli(
                "check", "updates",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("up to date")
        }

        @Test
        fun `security-check with no vulnerabilities`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            dependangerMockCleanup = mockDependangerProcessing()

            val result = runCli(
                "check", "security",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("No vulnerabilities found")
        }

        @Test
        fun `license-check with no violations`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            dependangerMockCleanup = mockDependangerProcessing()

            val result = runCli(
                "check", "license",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("No license violations found")
        }

        @Test
        fun `analyze with no compatibility issues`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            dependangerMockCleanup = mockDependangerProcessing()

            val result = runCli(
                "analyze",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("No compatibility issues found")
        }

        @Test
        fun `resolve-transitives with no transitives`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            dependangerMockCleanup = mockDependangerProcessing()

            val result = runCli(
                "resolve-transitives",
                "-i", metadataFile.toString()
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("No transitive dependencies found")
        }

        @Test
        fun `check-updates with json output format`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            dependangerMockCleanup = mockDependangerProcessing()

            val result = runCli(
                "check", "updates",
                "-i", metadataFile.toString(),
                "--format", "json"
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output.trim()).startsWith("[")
        }

        @Test
        fun `check-updates writes report to file`() {
            initMetadata()
            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )

            dependangerMockCleanup = mockDependangerProcessing()

            val reportFile = tempDir.resolve("updates-report.json")
            val result = runCli(
                "check", "updates",
                "-i", metadataFile.toString(),
                "-o", reportFile.toString()
            )
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(reportFile).exists()
        }
    }

    @Nested
    @DisplayName("Multiple Entity Interactions")
    inner class MultipleEntityInteractions {

        @Test
        fun `add multiple versions and libraries then verify metadata integrity`() {
            initMetadata()

            runCli("add", "version", "kotlin", "2.1.20", "-i", metadataFile.toString())
            runCli("add", "version", "ktor", "3.1.1", "-i", metadataFile.toString())
            runCli("add", "version", "coroutines", "1.10.1", "-i", metadataFile.toString())

            runCli(
                "add", "library", "stdlib", "org.jetbrains.kotlin:kotlin-stdlib",
                "--version-ref", "kotlin",
                "-i", metadataFile.toString()
            )
            runCli(
                "add", "library", "ktor-core", "io.ktor:ktor-client-core",
                "--version-ref", "ktor",
                "-i", metadataFile.toString()
            )
            runCli(
                "add", "library", "coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core",
                "--version-ref", "coroutines",
                "-i", metadataFile.toString()
            )

            runCli(
                "add", "plugin", "kotlin-jvm", "org.jetbrains.kotlin.jvm",
                "-v", "ref:kotlin",
                "-i", metadataFile.toString()
            )

            runCli(
                "add", "bundle", "kotlin-essentials",
                "--libraries", "stdlib,coroutines-core",
                "-i", metadataFile.toString()
            )

            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions).hasSize(3)
            assertThat(metadata.libraries).hasSize(3)
            assertThat(metadata.plugins).hasSize(1)
            assertThat(metadata.bundles).hasSize(1)
            assertThat(metadata.bundles.first().libraries).containsExactly("stdlib", "coroutines-core")
        }

        @Test
        fun `sequential add and remove operations maintain consistency`() {
            initMetadata()

            runCli("add", "version", "v1", "1.0.0", "-i", metadataFile.toString())
            runCli("add", "version", "v2", "2.0.0", "-i", metadataFile.toString())
            runCli("add", "version", "v3", "3.0.0", "-i", metadataFile.toString())

            runCli("remove", "version", "v2", "-i", metadataFile.toString())

            val metadata = CliTestSupport.readMetadata(metadataFile)
            assertThat(metadata.versions).hasSize(2)
            assertThat(metadata.versions.map { it.name }).containsExactlyInAnyOrder("v1", "v3")
        }
    }
}
