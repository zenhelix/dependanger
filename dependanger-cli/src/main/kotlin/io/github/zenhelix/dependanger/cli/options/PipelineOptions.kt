package io.github.zenhelix.dependanger.cli.options

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.CliDefaults

public class PipelineOptions : OptionGroup("Pipeline options") {
    public val input: String by option("-i", "--input", help = "Input metadata file")
        .default(CliDefaults.METADATA_FILE)
    public val distribution: String? by option("-d", "--distribution", help = "Active distribution")
    public val format: String by option("--format", help = "Output format: text, json")
        .default(CliDefaults.OUTPUT_FORMAT_TEXT)
}
