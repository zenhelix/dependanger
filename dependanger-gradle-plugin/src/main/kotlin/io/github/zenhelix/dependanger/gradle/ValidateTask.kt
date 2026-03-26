package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import kotlinx.coroutines.runBlocking
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

public abstract class ValidateTask : AbstractDependangerTask() {
    init {
        description = "Validate DSL configuration"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.dsl.toMetadata()
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata).build()
            val result = runBlocking { dependanger.validate() }

            result.diagnostics.errors.forEach { logger.error("Dependanger validation ERROR: ${it.message}") }
            result.diagnostics.warnings.forEach { logger.warn("Dependanger validation WARN: ${it.message}") }
            result.diagnostics.infos.forEach { logger.info("Dependanger validation INFO: ${it.message}") }

            val errCount = result.diagnostics.errors.size
            val warnCount = result.diagnostics.warnings.size

            logger.lifecycle("Dependanger: Validation completed: $errCount errors, $warnCount warnings")

            if (result.diagnostics.hasErrors && failOnError) {
                throw GradleException("Dependanger validation failed with $errCount error(s).")
            }
        }
    }
}
