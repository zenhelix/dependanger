package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.core.model.Diagnostics
import java.nio.file.Path

public class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate metadata.json or DSL configuration"

    public val input: String by option("-i", "--input", help = "Input file").default(CliDefaults.METADATA_FILE)
    public val strict: Boolean by option("--strict", help = "Fail on warnings").flag()
    public val format: String by option("--format", help = "Output format: text, json").default(CliDefaults.OUTPUT_FORMAT_TEXT)

    override fun run() {
        val jsonMode = format == CliDefaults.OUTPUT_FORMAT_JSON
        val formatter = OutputFormatter(jsonMode = jsonMode, terminal = terminal)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(input))

            val result = CoroutineRunner.run {
                Dependanger.fromMetadata(metadata).build().validate()
            }

            val diagnostics = result.diagnostics

            if (jsonMode) {
                formatter.renderJson(diagnostics, Diagnostics.serializer())
            } else {
                formatter.renderDiagnostics(diagnostics)
            }

            if (diagnostics.hasErrors || (strict && diagnostics.warnings.isNotEmpty())) {
                throw CliException.ValidationFailed(diagnostics)
            }

            formatter.success("Validation passed")
        }
    }
}
