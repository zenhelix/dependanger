package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.core.model.Diagnostics
import java.nio.file.Path

public class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate metadata.json or DSL configuration"

    private val opts by PipelineOptions()
    public val strict: Boolean by option("--strict", help = "Fail on warnings").flag()

    override fun run() {
        val runner = PipelineRunner(this, opts)
        val jsonMode = opts.format == CliDefaults.OUTPUT_FORMAT_JSON
        withErrorHandling(runner.formatter) {
            val metadata = MetadataService().read(Path.of(opts.input))
            val result = CoroutineRunner.run {
                Dependanger.fromMetadata(metadata).build().validate()
            }

            val diagnostics = result.diagnostics

            if (jsonMode) {
                runner.formatter.renderJson(diagnostics, Diagnostics.serializer())
            } else {
                runner.formatter.renderDiagnostics(diagnostics)
            }

            if (diagnostics.hasErrors || (strict && diagnostics.warnings.isNotEmpty())) {
                throw CliException.ValidationFailed(diagnostics)
            }

            runner.formatter.success("Validation passed")
        }
    }
}
