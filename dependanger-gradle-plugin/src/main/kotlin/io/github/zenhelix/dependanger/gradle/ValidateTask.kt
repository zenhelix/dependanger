package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.TaskAction

public abstract class ValidateTask : AbstractDependangerTask() {
    init {
        description = "Validate DSL configuration"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.toMetadata()
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata).build()
            val result = runBlocking { dependanger.validate() }

            val errCount = result.diagnostics.errors.size
            val warnCount = result.diagnostics.warnings.size
            logger.lifecycle("Dependanger: Validation completed: $errCount errors, $warnCount warnings")

            DependangerTaskHelper.handleProcessingErrors(
                result, failOnError, logger,
                errorMessage = "Dependanger validation failed with $errCount error(s).",
            )
        }
    }
}
