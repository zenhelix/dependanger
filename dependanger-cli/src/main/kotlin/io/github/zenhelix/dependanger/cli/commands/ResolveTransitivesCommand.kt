package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.api.transitives
import io.github.zenhelix.dependanger.api.versionConflicts
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettings
import io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettingsKey
import io.github.zenhelix.dependanger.feature.model.transitive.ConflictResolutionStrategy
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflict
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.writeText

@Serializable
public data class TransitiveResolutionOutput(
    val transitives: List<TransitiveTree>,
    val versionConflicts: List<VersionConflict>,
)

public class ResolveTransitivesCommand : CliktCommand(name = "resolve-transitives") {
    override fun help(context: Context): String = "Resolve transitive dependencies"

    private val opts by PipelineOptions()
    public val output: String? by option("-o", "--output", help = "Output file")
    public val depth: Int? by option("--depth", help = "Max resolution depth").int()
    public val includeOptional: Boolean by option("--include-optional", help = "Include optional deps").flag()
    public val repositories: String? by option("--repositories", help = "Maven repos (comma-separated)")
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val conflictResolution: String by option(
        "--conflict-resolution",
        help = "Strategy: HIGHEST,FIRST,FAIL"
    ).default(CliDefaults.CONFLICT_RESOLUTION_HIGHEST)

    override fun run(): Unit = PipelineRunner(this, opts).run(
        configure = {
            val strategy = parseEnum<ConflictResolutionStrategy>(conflictResolution, "conflict resolution strategy")
            preset(ProcessingPreset.STRICT)
            withContextProperty(TransitiveResolutionSettingsKey, TransitiveResolutionSettings(
                enabled = true,
                maxDepth = depth,
                includeOptional = includeOptional,
                conflictResolution = strategy,
                repositories = parseMavenRepositories(repositories) ?: emptyList(),
                cacheTtlHours = if (offline) Long.MAX_VALUE else TransitiveResolutionSettings.DEFAULT_CACHE_TTL_HOURS,
                maxTransitives = TransitiveResolutionSettings.DEFAULT.maxTransitives,
                scopes = TransitiveResolutionSettings.DEFAULT.scopes,
                cacheDirectory = null,
            ))
        },
        handle = { result ->
            val trees = result.transitives
            val conflicts = result.versionConflicts
            val combined = TransitiveResolutionOutput(transitives = trees, versionConflicts = conflicts)

            val jsonMode = opts.format == CliDefaults.OUTPUT_FORMAT_JSON

            if (jsonMode) {
                formatter.renderJson(combined, TransitiveResolutionOutput.serializer())
            } else {
                if (trees.isEmpty()) {
                    formatter.success("No transitive dependencies found")
                } else {
                    trees.forEach { tree ->
                        renderTree(formatter, tree, indent = 0)
                    }
                    formatter.info("${trees.size} dependency tree(s) resolved")
                }

                if (conflicts.isNotEmpty()) {
                    formatter.warning("${conflicts.size} version conflict(s) detected:")
                    formatter.renderTable(
                        headers = listOf("Group", "Artifact", "Requested Versions", "Resolved", "Strategy"),
                        rows = conflicts.map { conflict ->
                            listOf(
                                conflict.group,
                                conflict.artifact,
                                conflict.requestedVersions.joinToString(", "),
                                conflict.resolvedVersion,
                                conflict.resolution.name,
                            )
                        }
                    )
                }
            }

            output?.let { outputFile ->
                val outputPath = Path.of(outputFile)
                val jsonString = CliDefaults.CLI_JSON.encodeToString(TransitiveResolutionOutput.serializer(), combined)
                outputPath.writeText(jsonString)
                formatter.success("Report written to $outputPath")
            }
        }
    )

    private fun renderTree(formatter: OutputFormatter, tree: TransitiveTree, indent: Int) {
        val prefix = "  ".repeat(indent) + if (indent > 0) "+-- " else ""
        val version = tree.version ?: "?"
        val scope = if (tree.scope != null) " (${tree.scope})" else ""
        val markers = buildList {
            if (tree.isDuplicate) add("duplicate")
            if (tree.isCycle) add("cycle")
        }
        val markerSuffix = if (markers.isNotEmpty()) " [${markers.joinToString(", ")}]" else ""

        formatter.println("$prefix${tree.group}:${tree.artifact}:$version$scope$markerSuffix")

        if (!tree.isDuplicate && !tree.isCycle) {
            tree.children.forEach { child ->
                renderTree(formatter, child, indent + 1)
            }
        }
    }
}
