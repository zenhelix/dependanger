package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.JdkConstraints
import io.github.zenhelix.dependanger.core.model.Requirements

public class UpdateLibraryCommand : CliktCommand(name = "library") {
    override fun help(context: Context): String = "Update a library in metadata.json"

    public val alias: String by argument(help = "Library alias to update")
    public val version: String? by option("-v", "--version", help = "New version or ref:alias")
    public val tags: String? by option("-t", "--tags", help = "New tags (comma-separated)")
    public val requiresJdk: String? by option("--requires-jdk", help = "Minimum JDK version")
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        val existing = metadata.libraries.find { it.alias == alias }
            ?: throw CliException.AliasNotFound("Library", alias)

        val updatedLib = existing.copy(
            version = version?.let { parseVersionRef(it) } ?: existing.version,
            tags = tags?.let { parseCommaSeparated(it).toSet() } ?: existing.tags,
            requires = requiresJdk?.let {
                Requirements(
                    jdk = JdkConstraints(min = it.toIntOrNull(), max = null),
                    kotlin = null,
                )
            } ?: existing.requires,
        )

        val updated = metadata.copy(
            libraries = metadata.libraries.map { if (it.alias == alias) updatedLib else it }
        )

        updated to "Updated library '$alias'"
    }
}
