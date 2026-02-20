package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

public class GenerateCommand : CliktCommand(name = "generate") {
    override fun help(context: Context): String = "Generate artifacts from metadata"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val outputDir: String by option("-o", "--output-dir", help = "Output directory").default(".")
    public val toml: Boolean by option("--toml", help = "Generate TOML version catalog").flag()
    public val bom: Boolean by option("--bom", help = "Generate Maven BOM").flag()
    public val distribution: String? by option("-d", "--distribution", help = "Distribution profile")
    public val tomlFilename: String by option("--toml-filename", help = "TOML filename").default(CliDefaults.TOML_FILENAME)
    public val tomlComments: Boolean by option("--toml-comments", help = "Include comments").flag()
    public val tomlSort: Boolean by option("--toml-sort", help = "Sort sections").flag(default = true)
    public val tomlInlineVersions: Boolean by option("--toml-inline-versions", help = "Inline versions").flag()
    public val bomGroup: String? by option("--bom-group", help = "BOM groupId")
    public val bomArtifact: String? by option("--bom-artifact", help = "BOM artifactId")
    public val bomVersion: String? by option("--bom-version", help = "BOM version")
    public val bomIncludeOptional: Boolean by option("--bom-include-optional", help = "Include optional").flag()

    override fun run(): Unit = TODO()
}
