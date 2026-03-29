package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.transitives
import io.github.zenhelix.dependanger.api.versionConflicts
import io.github.zenhelix.dependanger.core.model.ConflictResolutionStrategy
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.features.transitive.model.TransitiveTree
import io.github.zenhelix.dependanger.features.transitive.model.VersionConflict
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

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file")
    public val format: String by option("--format", help = "Format: json, text").default(CliDefaults.OUTPUT_FORMAT_TEXT)
    public val depth: Int? by option("--depth", help = "Max resolution depth").int()
    public val includeOptional: Boolean by option("--include-optional", help = "Include optional deps").flag()
    public val repositories: String? by option("--repositories", help = "Maven repos (comma-separated)")
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val conflictResolution: String by option(
        "--conflict-resolution",
        help = "Strategy: HIGHEST,FIRST,FAIL"
    ).default(CliDefaults.CONFLICT_RESOLUTION_HIGHEST)

    override fun run() {
        val jsonMode = format == CliDefaults.OUTPUT_FORMAT_JSON
        val formatter = OutputFormatter(jsonMode = jsonMode, terminal = terminal)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(input))

            val strategy = try {
                ConflictResolutionStrategy.valueOf(conflictResolution.uppercase())
            } catch (_: IllegalArgumentException) {
                throw CliException.InvalidArgument(
                    "Unknown conflict resolution strategy '$conflictResolution'. Available: ${ConflictResolutionStrategy.entries.joinToString { it.name }}"
                )
            }

            val updatedSettings = metadata.settings.copy(
                transitiveResolution = metadata.settings.transitiveResolution.copy(
                    enabled = true,
                    maxDepth = depth ?: metadata.settings.transitiveResolution.maxDepth,
                    includeOptional = includeOptional,
                    conflictResolution = strategy,
                    repositories = parseMavenRepositories(repositories) ?: metadata.settings.transitiveResolution.repositories,
                    cacheTtlHours = if (offline) Long.MAX_VALUE else metadata.settings.transitiveResolution.cacheTtlHours,
                )
            )
            val updatedMetadata = metadata.copy(settings = updatedSettings)

            val dependanger = Dependanger.fromMetadata(updatedMetadata)
                .preset(ProcessingPreset.STRICT)
                .build()

            val result = CoroutineRunner.run {
                dependanger.process()
            }

            val trees = result.transitives
            val conflicts = result.versionConflicts
            val combined = TransitiveResolutionOutput(transitives = trees, versionConflicts = conflicts)

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
    }

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
