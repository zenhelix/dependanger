package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_PARALLELISM
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_TIMEOUT_MS
import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettingsKey
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import kotlinx.serialization.builtins.ListSerializer

public class CheckUpdatesCommand : CliktCommand(name = "updates") {
    override fun help(context: Context): String = "Check for available library updates"

    private val opts by PipelineOptions()
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val includePrerelease: Boolean by option("--include-prerelease", help = "Include prerelease").flag()
    public val exclude: List<String> by option("--exclude", help = "Exclude patterns").multiple()
    public val type: String? by option("--type", help = "Update types: PATCH,MINOR,MAJOR")
    public val repositories: String? by option("--repositories", help = "Maven repos (comma-separated)")
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val failOnUpdates: Boolean by option("--fail-on-updates", help = "Fail if updates found").flag()

    override fun run(): Unit = PipelineRunner(this, opts).run(
        configure = {
            preset(ProcessingPreset.STRICT)
            withContextProperty(
                UpdateCheckSettingsKey, UpdateCheckSettings(
                    enabled = true,
                    includePrerelease = includePrerelease,
                    excludePatterns = exclude,
                    repositories = parseMavenRepositories(repositories) ?: emptyList(),
                    cacheTtlHours = if (offline) Long.MAX_VALUE else UpdateCheckSettings.DEFAULT.cacheTtlHours,
                    timeout = NETWORK_DEFAULT_TIMEOUT_MS,
                    parallelism = NETWORK_DEFAULT_PARALLELISM,
                    cacheDirectory = null,
                )
            )
        },
        handle = { result ->
            val updates = result.updates

            val filteredUpdates = type?.let { typeFilter ->
                val allowedTypes = parseCommaSeparated(typeFilter).map { it.uppercase() }.toSet()
                updates.filter { it.updateType.name in allowedTypes }
            } ?: updates

            renderItems(
                items = filteredUpdates,
                serializer = UpdateAvailableInfo.serializer(),
                headers = listOf("Library", "Current", "Available", "Type"),
                rowMapper = { update ->
                    listOf(
                        update.coordinate.toString(),
                        update.currentVersion,
                        update.latestVersion,
                        update.updateType.name,
                    )
                },
                emptyMessage = "All libraries are up to date",
                itemNoun = "update(s) available",
            )

            writeOutputIfRequested(output, filteredUpdates, ListSerializer(UpdateAvailableInfo.serializer()))

            if (failOnUpdates && filteredUpdates.isNotEmpty()) {
                throw ProgramResult(1)
            }
        }
    )
}
