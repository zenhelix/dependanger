package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

public class InitCommand : CliktCommand(name = "init") {
    override fun help(context: Context): String = "Create a new metadata.json with initial structure"

    public val output: String by option("-o", "--output", help = "Output file path").default(CliDefaults.METADATA_FILE)
    public val format: String by option("--format", help = "Format: json, yaml").default("json")
    public val force: Boolean by option("-f", "--force", help = "Overwrite existing file").flag()

    override fun run(): Unit = TODO()
}
