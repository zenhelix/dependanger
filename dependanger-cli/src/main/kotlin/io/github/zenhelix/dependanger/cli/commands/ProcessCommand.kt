package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import java.nio.file.Path
import kotlin.io.path.writeText

public class ProcessCommand : CliktCommand(name = "process") {
    override fun help(context: Context): String = "Run processing pipeline"

    private val opts by PipelineOptions()
    public val output: String by option("-o", "--output", help = "Output effective file").default(CliDefaults.EFFECTIVE_OUTPUT_FILE)
    public val preset: String by option("--preset", help = "Processing preset").default(CliDefaults.PROCESSING_PRESET_DEFAULT)
    public val disableProcessor: List<String> by option("--disable-processor", help = "Disable a processor by ID").multiple()

    override fun run(): Unit = PipelineRunner(this, opts).run(
        configure = {
            val resolvedPreset = parseEnum<ProcessingPreset>(preset, "preset")
            preset(resolvedPreset)
            disableProcessor.forEach { id -> disableProcessor(id) }
        },
        handle = { result ->
            val effective = result.effectiveOrNull()
            if (effective == null) {
                formatter.renderDiagnostics(result.diagnostics)
                throw CliException.ValidationFailed(result.diagnostics)
            }

            val outputPath = Path.of(output)
            val jsonString = CliDefaults.CLI_JSON.encodeToString(EffectiveMetadata.serializer(), effective)
            outputPath.writeText(jsonString)

            formatter.renderDiagnostics(result.diagnostics)
            if (result.isSuccess) {
                formatter.success("Processed metadata written to $outputPath")
            } else {
                formatter.warning("Processed metadata written to $outputPath (with errors)")
            }
        }
    )
}
