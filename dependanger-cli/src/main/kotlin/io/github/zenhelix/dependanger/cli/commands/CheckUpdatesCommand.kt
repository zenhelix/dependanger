package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettingsKey
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Path
import kotlin.io.path.writeText

public class CheckUpdatesCommand : CliktCommand(name = "updates") {
    override fun help(context: Context): String = "Check for available library updates"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val format: String by option("--format", help = "Output format").default(CliDefaults.OUTPUT_FORMAT_TEXT)
    public val includePrerelease: Boolean by option("--include-prerelease", help = "Include prerelease").flag()
    public val exclude: List<String> by option("--exclude", help = "Exclude patterns").multiple()
    public val type: String? by option("--type", help = "Update types: PATCH,MINOR,MAJOR")
    public val repositories: String? by option("--repositories", help = "Maven repos (comma-separated)")
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val failOnUpdates: Boolean by option("--fail-on-updates", help = "Fail if updates found").flag()

    override fun run() {
        val jsonMode = format == CliDefaults.OUTPUT_FORMAT_JSON
        val formatter = OutputFormatter(jsonMode = jsonMode, terminal = terminal)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(input))

            val dependanger = Dependanger.fromMetadata(metadata)
                .preset(ProcessingPreset.STRICT)
                .withContextProperty(UpdateCheckSettingsKey, UpdateCheckSettings(
                    enabled = true,
                    includePrerelease = includePrerelease,
                    excludePatterns = exclude,
                    repositories = parseMavenRepositories(repositories) ?: emptyList(),
                    cacheTtlHours = if (offline) Long.MAX_VALUE else UpdateCheckSettings.DEFAULT_CACHE_TTL_HOURS,
                    timeout = UpdateCheckSettings.DEFAULT_TIMEOUT_MS,
                    parallelism = UpdateCheckSettings.DEFAULT_PARALLELISM,
                    cacheDirectory = null,
                ))
                .build()

            val result = CoroutineRunner.run {
                dependanger.process()
            }

            val updates = result.updates

            val filteredUpdates = type?.let { typeFilter ->
                val allowedTypes = parseCommaSeparated(typeFilter).map { it.uppercase() }.toSet()
                updates.filter { it.updateType.name in allowedTypes }
            } ?: updates

            if (jsonMode) {
                formatter.renderJson(filteredUpdates, ListSerializer(UpdateAvailableInfo.serializer()))
            } else {
                if (filteredUpdates.isEmpty()) {
                    formatter.success("All libraries are up to date")
                } else {
                    formatter.renderTable(
                        headers = listOf("Library", "Current", "Available", "Type"),
                        rows = filteredUpdates.map { update ->
                            listOf(
                                "${update.group}:${update.artifact}",
                                update.currentVersion,
                                update.latestVersion,
                                update.updateType.name,
                            )
                        }
                    )
                    formatter.info("${filteredUpdates.size} update(s) available")
                }
            }

            output?.let { outputFile ->
                val outputPath = Path.of(outputFile)
                val jsonString = CliDefaults.CLI_JSON.encodeToString(ListSerializer(UpdateAvailableInfo.serializer()), filteredUpdates)
                outputPath.writeText(jsonString)
                formatter.success("Report written to $outputPath")
            }

            if (failOnUpdates && filteredUpdates.isNotEmpty()) {
                throw ProgramResult(1)
            }
        }
    }
}
