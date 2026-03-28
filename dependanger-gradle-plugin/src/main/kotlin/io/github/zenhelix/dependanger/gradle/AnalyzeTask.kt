package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.effective.ProcessorIds
import kotlinx.coroutines.runBlocking
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

public abstract class AnalyzeTask : AbstractDependangerTask() {
    init {
        description = "Analyze dependency compatibility"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.dsl.toMetadata()
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata)
                .jdkVersion(Runtime.version().feature())
                .configureProcessing { enableOptional(ProcessorIds.COMPATIBILITY_ANALYSIS) }
                .build()

            val result = runBlocking { dependanger.process() }

            val errors = result.diagnostics.errors.filter { it.code.startsWith("COMPAT") }
            val warnings = result.diagnostics.warnings.filter { it.code.startsWith("COMPAT") }

            if (errors.isEmpty() && warnings.isEmpty()) {
                logger.lifecycle("Dependanger: No compatibility issues found.")
            } else {
                logger.error("Dependanger COMPAT ERROR: ${errors.joinToString("\n") { it.message }}")
                logger.warn("Dependanger COMPAT WARN: ${warnings.joinToString("\n") { it.message }}")
                logger.lifecycle("Dependanger: Analysis complete: ${errors.size} errors, ${warnings.size} warnings")
            }

            if (errors.isNotEmpty() && failOnError) {
                throw GradleException("Dependanger: Compatibility analysis found ${errors.size} error(s).")
            }
        }
    }
}
