package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

public class ProcessCommand : CliktCommand(name = "process") {
    override fun help(context: Context): String = "Run processing pipeline"

    public val input: String by option("-i", "--input", help = "Input metadata file").default("metadata.json")
    public val output: String by option("-o", "--output", help = "Output effective file").default("effective.json")
    public val preset: String by option("--preset", help = "Processing preset").default("DEFAULT")
    public val distribution: String? by option("-d", "--distribution", help = "Active distribution")
    public val importBoms: Boolean by option("--import-boms", help = "Import BOM versions").flag()
    public val noImportBoms: Boolean by option("--no-import-boms", help = "Disable BOM import").flag()
    public val targetJdk: Int? by option("--target-jdk", help = "Target JDK version").int()

    override fun run(): Unit = TODO()
}
