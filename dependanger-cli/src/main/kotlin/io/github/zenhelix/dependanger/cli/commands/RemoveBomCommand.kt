package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemoveBomCommand : CliktCommand(name = "bom") {
    override fun help(context: Context): String = "Remove a BOM import from metadata.json"

    public val alias: String by argument(help = "BOM alias to remove")
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        if (metadata.bomImports.none { it.alias == alias }) {
            throw CliException.AliasNotFound("BOM", alias)
        }

        val updated = metadata.copy(bomImports = metadata.bomImports.filter { it.alias != alias })

        updated to "Removed BOM '$alias'"
    }
}
