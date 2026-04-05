package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Distribution
import io.github.zenhelix.dependanger.core.model.filter.BundleFilter
import io.github.zenhelix.dependanger.core.model.filter.LibraryFilterSpec
import io.github.zenhelix.dependanger.core.model.filter.TagExclude
import io.github.zenhelix.dependanger.core.model.filter.TagFilter
import io.github.zenhelix.dependanger.core.model.filter.TagInclude

public class AddDistributionCommand : CliktCommand(name = "distribution") {
    override fun help(context: Context): String = "Add a distribution to metadata.json"

    public val name: String by argument(help = "Distribution name")
    public val includeTags: String? by option("--include-tags", help = "Tags to include (comma-separated)")
    public val excludeTags: String? by option("--exclude-tags", help = "Tags to exclude (comma-separated)")
    public val includeBundles: String? by option("--include-bundles", help = "Bundles to include (comma-separated)")
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
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

        val librarySpec = if (tagFilter != null || bundleFilter != null) {
            LibraryFilterSpec(
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

        val newDistribution = Distribution(name = name, librarySpec = librarySpec, pluginSpec = null)
        val updated = metadata.copy(distributions = metadata.distributions + newDistribution)

        updated to "Added distribution '$name'"
    }
}
