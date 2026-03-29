package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Path
import kotlin.io.path.exists

public class InitCommand : CliktCommand(name = "init") {
    override fun help(context: Context): String = "Create a new metadata.json with initial structure"

    public val output: String by option("-o", "--output", help = "Output file path").default(CliDefaults.METADATA_FILE)
    public val force: Boolean by option("-f", "--force", help = "Overwrite existing file").flag()

    override fun run() {
        val formatter = OutputFormatter(jsonMode = false)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val outputPath = Path.of(output)

            if (outputPath.exists() && !force) {
                throw CliException.FileAlreadyExists(outputPath.toString())
            }

            val metadata = metadataService.emptyMetadata()
            metadataService.write(metadata, outputPath)

            formatter.success("Created $outputPath")
        }
    }
}
