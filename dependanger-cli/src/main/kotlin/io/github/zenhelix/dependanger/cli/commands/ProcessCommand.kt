package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option

public class ProcessCommand : CliktCommand(name = "process") {
    override fun help(context: Context): String = "Run processing pipeline"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String by option("-o", "--output", help = "Output effective file").default(CliDefaults.EFFECTIVE_OUTPUT_FILE)
    public val preset: String by option("--preset", help = "Processing preset").default(CliDefaults.PROCESSING_PRESET_DEFAULT)
    public val distribution: String? by option("-d", "--distribution", help = "Active distribution")
    public val disableProcessor: List<String> by option("--disable-processor", help = "Disable a processor by ID").multiple()

    override fun run(): Unit = TODO()
}
