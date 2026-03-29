package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.writeBomTo
import io.github.zenhelix.dependanger.api.writeTomlTo
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import java.nio.file.Path

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

    override fun run() {
        val formatter = OutputFormatter(jsonMode = false)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val generateToml = toml || !bom
            val generateBom = bom

            val metadata = metadataService.read(Path.of(input))

            val dependanger = Dependanger.fromMetadata(metadata).build()
            val result = CoroutineRunner.run {
                dependanger.process(distribution)
            }

            if (!result.isSuccess) {
                formatter.renderDiagnostics(result.diagnostics)
                throw CliException.ValidationFailed(result.diagnostics)
            }

            val outDir = Path.of(outputDir)

            if (generateToml) {
                val tomlConfig = TomlConfig(
                    filename = tomlFilename,
                    includeComments = tomlComments,
                    sortSections = tomlSort,
                    useInlineVersions = tomlInlineVersions,
                    includeDeprecationComments = true,
                )
                val tomlPath = outDir.resolve(tomlConfig.filename)
                result.writeTomlTo(tomlPath, tomlConfig)
                formatter.success("Generated TOML version catalog: $tomlPath")
            }

            if (generateBom) {
                val resolvedGroup = bomGroup
                    ?: throw CliException.InvalidArgument("--bom-group is required for BOM generation")
                val resolvedArtifact = bomArtifact
                    ?: throw CliException.InvalidArgument("--bom-artifact is required for BOM generation")
                val resolvedVersion = bomVersion
                    ?: throw CliException.InvalidArgument("--bom-version is required for BOM generation")

                val bomConfig = BomConfig(
                    groupId = resolvedGroup,
                    artifactId = resolvedArtifact,
                    version = resolvedVersion,
                    name = null,
                    description = null,
                    filename = BomConfig.DEFAULT_FILENAME,
                    includeOptionalDependencies = bomIncludeOptional,
                    prettyPrint = true,
                    includeDeprecationComments = true,
                )
                val bomPath = outDir.resolve(bomConfig.filename)
                result.writeBomTo(bomPath, bomConfig)
                formatter.success("Generated Maven BOM: $bomPath")
            }
        }
    }
}
