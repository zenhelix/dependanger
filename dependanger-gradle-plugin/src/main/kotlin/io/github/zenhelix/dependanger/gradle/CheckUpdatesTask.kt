package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.features.updates.UpdateCheckProcessor
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.TaskAction

public abstract class CheckUpdatesTask : AbstractDependangerTask() {
    init {
        description = "Check for available dependency updates"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.dsl.toMetadata()
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata)
                .configureProcessing { enableOptional(UpdateCheckProcessor.PROCESSOR_ID) }
                .build()

            val result = runBlocking { dependanger.process() }

            if (!result.isSuccess) {
                DependangerTaskHelper.handleProcessingErrors(result, failOnError, logger)
                return@runWithErrorHandling
            }

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
    }
}
