package io.github.zenhelix.dependanger.cli.options

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.CliDefaults

public class MetadataOptions : OptionGroup("Metadata file options") {
    public val input: String by option("-i", "--input", help = "Input metadata file")
        .default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")
}
