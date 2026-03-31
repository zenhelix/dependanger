package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public abstract class GenerateMetadataTask : AbstractDependangerTask() {
    init {
        description = "Generate metadata.json from DSL"
        // DSL configuration is not trackable as an input — task always re-executes
        outputs.upToDateWhen { false }
    }

    @get:OutputFile
    public val metadataFile: RegularFileProperty = project.objects.fileProperty().convention(
        extension.outputDirectory.file(DependangerTaskHelper.METADATA_FILE)
    )

    @TaskAction
    public fun execute() {
        val metadata = try {
            extension.dsl.toMetadata()
        } catch (e: Exception) {
            throw GradleException("Dependanger: Failed to evaluate DSL configuration: ${e.message}", e)
        }

        val outputFile = metadataFile.get().asFile

        try {
            outputFile.parentFile?.mkdirs()
            val format = JsonSerializationFormat()
            format.write(metadata, outputFile.toPath())

            logger.lifecycle("Dependanger: Generated ${DependangerTaskHelper.METADATA_FILE} -> $outputFile")
            logger.lifecycle("  Versions: ${metadata.versions.size}")
            logger.lifecycle("  Libraries: ${metadata.libraries.size}")
            logger.lifecycle("  Plugins: ${metadata.plugins.size}")
            logger.lifecycle("  Bundles: ${metadata.bundles.size}")
        } catch (e: Exception) {
            throw GradleException("Dependanger: Failed to write ${DependangerTaskHelper.METADATA_FILE}: ${e.message}", e)
        }
    }
}
