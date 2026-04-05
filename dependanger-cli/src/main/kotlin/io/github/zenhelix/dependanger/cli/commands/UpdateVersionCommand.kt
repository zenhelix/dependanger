package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.VersionReference

public class UpdateVersionCommand : CliktCommand(name = "version") {
    override fun help(context: Context): String = "Update a version in metadata.json"

    public val alias: String by argument(help = "Version or library alias")
    public val version: String by argument(help = "New version value")
    public val library: Boolean by option("-l", "--library", help = "Update library version").flag()
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        val updated = if (library) {
            if (metadata.libraries.none { it.alias == alias }) {
                throw CliException.AliasNotFound("Library", alias)
            }
            metadata.copy(
                libraries = metadata.libraries.map { lib ->
                    if (lib.alias == alias) lib.copy(version = VersionReference.Literal(version)) else lib
                }
            )
        } else {
            if (metadata.versions.none { it.name == alias }) {
                throw CliException.AliasNotFound("Version", alias)
            }
            metadata.copy(
                versions = metadata.versions.map { ver ->
                    if (ver.name == alias) ver.copy(value = version) else ver
                }
            )
        }

        val entity = if (library) "library" else "version"
        updated to "Updated $entity '$alias' to '$version'"
    }
}
