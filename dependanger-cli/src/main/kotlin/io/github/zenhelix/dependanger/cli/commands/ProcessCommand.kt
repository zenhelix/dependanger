package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import java.nio.file.Path
import kotlin.io.path.writeText

public class ProcessCommand : CliktCommand(name = "process") {
    override fun help(context: Context): String = "Run processing pipeline"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String by option("-o", "--output", help = "Output effective file").default(CliDefaults.EFFECTIVE_OUTPUT_FILE)
    public val preset: String by option("--preset", help = "Processing preset").default(CliDefaults.PROCESSING_PRESET_DEFAULT)
    public val distribution: String? by option("-d", "--distribution", help = "Active distribution")
    public val disableProcessor: List<String> by option("--disable-processor", help = "Disable a processor by ID").multiple()

    override fun run() {
        val formatter = OutputFormatter(jsonMode = false)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(input))

            val resolvedPreset = try {
                ProcessingPreset.valueOf(preset)
            } catch (_: IllegalArgumentException) {
                throw CliException.InvalidArgument(
                    "Unknown preset '$preset'. Available: ${ProcessingPreset.entries.joinToString { it.name }}"
                )
            }

            val builder = Dependanger.fromMetadata(metadata).preset(resolvedPreset)
            disableProcessor.forEach { id -> builder.disableProcessor(id) }
            val dependanger = builder.build()

            val result = CoroutineRunner.run {
                dependanger.process(distribution)
            }

            if (!result.isSuccess) {
                formatter.renderDiagnostics(result.diagnostics)
                throw CliException.ValidationFailed(result.diagnostics)
            }

            val outputPath = Path.of(output)
            val jsonString = CliDefaults.CLI_JSON.encodeToString(EffectiveMetadata.serializer(), result.effective!!)
            outputPath.writeText(jsonString)

            formatter.success("Processed metadata written to $outputPath")
        }
    }
}
