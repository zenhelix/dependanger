package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Library
import io.github.zenhelix.dependanger.core.model.VersionReference

public class AddLibraryCommand : CliktCommand(name = "library") {
    override fun help(context: Context): String = "Add a library to metadata.json"

    public val alias: String by argument(help = "Library alias")
    public val coordinates: String by argument(help = "Maven coordinates (group:artifact[:version])")
    public val version: String? by option("-v", "--version", help = "Library version")
    public val versionRef: String? by option("--version-ref", help = "Named version reference")
    public val tags: String? by option("-t", "--tags", help = "Tags (comma-separated)")
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        val coords = parseMavenCoordinates(coordinates)

        val resolvedVersion: VersionReference? = when {
            versionRef != null -> VersionReference.Reference(name = versionRef!!)
            version != null    -> VersionReference.Literal(version = version!!)
            else               -> coords.version?.let { VersionReference.Literal(version = it) }
        }

        if (metadata.libraries.any { it.alias == alias }) {
            throw CliException.DuplicateAlias("Library", alias)
        }

        val newLibrary = Library(
            alias = alias,
            group = coords.group,
            artifact = coords.artifact,
            version = resolvedVersion,
            description = null,
            tags = parseCommaSeparated(tags).toSet(),
            requires = null,
            deprecation = null,
            license = null,
            constraints = emptyList(),
            isPlatform = false,
        )
        val updated = metadata.copy(libraries = metadata.libraries + newLibrary)

        updated to "Added library '$alias'"
    }
}
