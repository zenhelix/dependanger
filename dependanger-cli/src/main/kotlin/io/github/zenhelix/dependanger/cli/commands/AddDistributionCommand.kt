package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.core.model.Distribution
import io.github.zenhelix.dependanger.core.model.filter.BundleFilter
import io.github.zenhelix.dependanger.core.model.filter.FilterSpec
import io.github.zenhelix.dependanger.core.model.filter.TagExclude
import io.github.zenhelix.dependanger.core.model.filter.TagFilter
import io.github.zenhelix.dependanger.core.model.filter.TagInclude
import java.nio.file.Path

public class AddDistributionCommand : CliktCommand(name = "add-distribution") {
    override fun help(context: Context): String = "Add a distribution to metadata.json"

    public val name: String by argument(help = "Distribution name")
    public val includeTags: String? by option("--include-tags", help = "Tags to include (comma-separated)")
    public val excludeTags: String? by option("--exclude-tags", help = "Tags to exclude (comma-separated)")
    public val includeBundles: String? by option("--include-bundles", help = "Bundles to include (comma-separated)")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")

    override fun run() {
        val formatter = OutputFormatter()
        val metadataService = MetadataService()
        withErrorHandling(formatter) {
            val inputPath = Path.of(input)
            val outputPath = Path.of(output ?: input)
            val metadata = metadataService.read(inputPath)

            if (metadata.distributions.any { it.name == name }) {
                throw CliException.DuplicateAlias("Distribution", name)
            }

            val tagFilter = if (includeTags != null || excludeTags != null) {
                TagFilter(
                    includes = if (includeTags != null) {
                        listOf(TagInclude(anyOf = parseCommaSeparated(includeTags).toSet(), allOf = emptySet()))
                    } else {
                        emptyList()
                    },
                    excludes = if (excludeTags != null) {
                        listOf(TagExclude(anyOf = parseCommaSeparated(excludeTags).toSet()))
                    } else {
                        emptyList()
                    },
                )
            } else {
                null
            }

            val bundleFilter = if (includeBundles != null) {
                BundleFilter(
                    includes = parseCommaSeparated(includeBundles).toSet(),
                    excludes = emptySet(),
                )
            } else {
                null
            }

            val spec = if (tagFilter != null || bundleFilter != null) {
                FilterSpec(
                    byTags = tagFilter,
                    byGroups = null,
                    byAliases = null,
                    byBundles = bundleFilter,
                    byDeprecated = null,
                    customFilters = emptyMap(),
                )
            } else {
                null
            }

            val newDistribution = Distribution(name = name, spec = spec)
            val updated = metadata.copy(distributions = metadata.distributions + newDistribution)

            metadataService.write(updated, outputPath)
            formatter.success("Added distribution '$name'")
        }
    }
}
