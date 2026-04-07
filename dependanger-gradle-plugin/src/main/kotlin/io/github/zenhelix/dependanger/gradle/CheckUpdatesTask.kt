package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import org.gradle.api.tasks.TaskAction

public abstract class CheckUpdatesTask : AbstractDependangerTask() {
    init {
        description = "Check for available dependency updates"
    }

    @TaskAction
    public fun execute(): Unit = AnalyticalTaskRunner(extension, logger).run(
        configure = { configureProcessing { enableOptional(FeatureProcessorIds.UPDATE_CHECK) } },
        handle = { result ->
            val updates = result.updates

            if (updates.isEmpty()) {
                logger.lifecycle("Dependanger: All dependencies are up to date.")
            } else {
                logger.lifecycle("Dependanger: Available updates:")
                updates.forEach { update ->
                    logger.lifecycle("  ${update.alias}: ${update.currentVersion} -> ${update.latestVersion} (${update.updateType})")
                }
                logger.lifecycle("  Total: ${updates.size} updates available")
            }
        }
    )
}
