package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.effective.serialization.EffectiveJsonFormat
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import kotlinx.coroutines.runBlocking
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

public abstract class GenerateEffectiveTask : AbstractDependangerTask() {
    init {
        description = "Generate effective metadata from DSL through processing pipeline"
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public val metadataFile: RegularFileProperty = project.objects.fileProperty().convention(
        extension.outputDirectory.file(DependangerTaskHelper.METADATA_FILE)
    )

    @get:OutputFile
    public val effectiveFile: RegularFileProperty = project.objects.fileProperty().convention(
        extension.outputDirectory.file(DependangerTaskHelper.EFFECTIVE_FILE)
    )

    @TaskAction
    public fun execute() {
        val inputFile = metadataFile.get().asFile
        if (!inputFile.exists()) {
            throw GradleException("${DependangerTaskHelper.METADATA_FILE} not found at $inputFile. Ensure dependangerGenerateMetadata has been executed.")
        }

        val format = JsonSerializationFormat()
        val metadata = format.read(inputFile.toPath())
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata).build()
            val result = runBlocking { dependanger.process() }

            DependangerTaskHelper.handleProcessingErrors(result, failOnError, logger)

            when (result) {
                is DependangerResult.Success, is DependangerResult.CompletedWithErrors -> {
                    val effective = checkNotNull(result.effectiveOrNull()) { "Effective result expected for ${result::class.simpleName}" }
                    val outputFile = effectiveFile.get().asFile
                    outputFile.parentFile?.mkdirs()
                    val effectiveFormat = EffectiveJsonFormat()
                    effectiveFormat.write(effective, outputFile.toPath())
                    logger.lifecycle("Dependanger: Generated ${DependangerTaskHelper.EFFECTIVE_FILE} -> $outputFile")
                }

                is DependangerResult.Failure -> {
                    logger.warn("Dependanger: Processing failed — skipping ${DependangerTaskHelper.EFFECTIVE_FILE} write")
                }
            }
        }
    }
}
