package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.clikt.testing.CliktCommandTestResult
import io.github.zenhelix.dependanger.core.model.Bundle
import io.github.zenhelix.dependanger.core.model.Library
import io.github.zenhelix.dependanger.core.model.Plugin
import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.Version
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import java.nio.file.Path

object CliTestSupport {

    fun runCli(vararg args: String): CliktCommandTestResult {
        val cli = DependangerCli().subcommands(
            InitCommand(),
            AddVersionCommand(),
            AddLibraryCommand(),
            AddPluginCommand(),
            AddBundleCommand(),
            AddBomCommand(),
            AddDistributionCommand(),
            RemoveVersionCommand(),
            RemoveLibraryCommand(),
            RemovePluginCommand(),
            RemoveBundleCommand(),
            RemoveBomCommand(),
            RemoveDistributionCommand(),
            UpdateVersionCommand(),
            UpdateLibraryCommand(),
            MigrateDeprecatedCommand(),
            ValidateCommand(),
            ProcessCommand(),
            GenerateCommand(),
        )
        return cli.test(argv = args.toList())
    }

    private val format = JsonSerializationFormat()

    fun writeMetadata(dir: Path, metadata: DependangerMetadata): Path {
        val file = dir.resolve("metadata.json")
        format.write(metadata, file)
        return file
    }

    fun readMetadata(path: Path): DependangerMetadata = format.read(path)

    fun emptyMetadata(): DependangerMetadata = DependangerMetadata(
        schemaVersion = DependangerMetadata.SCHEMA_VERSION,
        versions = emptyList(),
        libraries = emptyList(),
        plugins = emptyList(),
        bundles = emptyList(),
        bomImports = emptyList(),
        constraints = emptyList(),
        targetPlatforms = emptyList(),
        distributions = emptyList(),
        compatibility = emptyList(),
        settings = Settings.DEFAULT,
        presets = emptyList(),
        extensions = emptyMap(),
    )

    fun minimalMetadata(): DependangerMetadata = emptyMetadata().copy(
        versions = listOf(
            Version(name = "kotlin", value = "2.1.20", fallbacks = emptyList()),
            Version(name = "coroutines", value = "1.10.1", fallbacks = emptyList()),
        ),
        libraries = listOf(
            Library(
                alias = "stdlib",
                group = "org.jetbrains.kotlin",
                artifact = "kotlin-stdlib",
                version = VersionReference.Reference(name = "kotlin"),
                description = null,
                tags = setOf("kotlin", "core"),
                requires = null,
                deprecation = null,
                license = null,
                constraints = emptyList(),
                isPlatform = false,
            ),
            Library(
                alias = "coroutines",
                group = "org.jetbrains.kotlinx",
                artifact = "kotlinx-coroutines-core",
                version = VersionReference.Reference(name = "coroutines"),
                description = null,
                tags = setOf("kotlin"),
                requires = null,
                deprecation = null,
                license = null,
                constraints = emptyList(),
                isPlatform = false,
            ),
        ),
        plugins = listOf(
            Plugin(
                alias = "kotlin-jvm",
                id = "org.jetbrains.kotlin.jvm",
                version = VersionReference.Reference(name = "kotlin"),
                tags = emptySet(),
            ),
        ),
        bundles = listOf(
            Bundle(alias = "kotlin-essentials", libraries = listOf("stdlib", "coroutines"), extends = emptyList()),
        ),
    )
}
