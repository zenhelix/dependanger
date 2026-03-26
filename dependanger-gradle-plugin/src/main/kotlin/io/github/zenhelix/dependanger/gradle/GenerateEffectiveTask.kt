package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.effective.serialization.EffectiveJsonFormat
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.TaskAction

public abstract class GenerateEffectiveTask : AbstractDependangerTask() {
    init {
        description = "Generate effective metadata from DSL through processing pipeline"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.dsl.toMetadata()
        val outputDir = DependangerTaskHelper.ensureOutputDir(extension)
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata).build()
            val result = runBlocking { dependanger.process() }

            DependangerTaskHelper.handleProcessingErrors(result, failOnError, logger)

            val effective = result.effective
            if (effective != null) {
                val effectiveFormat = EffectiveJsonFormat()
                val outputFile = outputDir.resolve(DependangerTaskHelper.EFFECTIVE_FILE)
                effectiveFormat.write(effective, outputFile.toPath())
                logger.lifecycle("Dependanger: Generated ${DependangerTaskHelper.EFFECTIVE_FILE} -> $outputFile")
            } else {
                logger.warn("Dependanger: Processing completed but no effective metadata produced — skipping ${DependangerTaskHelper.EFFECTIVE_FILE} write")
            }
        }
    }
}
