package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

public abstract class GenerateMetadataTask : AbstractDependangerTask() {
    init {
        description = "Generate metadata.json from DSL"
    }

    @TaskAction
    public fun execute() {
        val metadata = try {
            extension.dsl.toMetadata()
        } catch (e: Exception) {
            throw GradleException("Dependanger: Failed to evaluate DSL configuration: ${e.message}", e)
        }

        val outputDir = DependangerTaskHelper.ensureOutputDir(extension)

        try {
            val format = JsonSerializationFormat()
            val outputFile = outputDir.resolve(DependangerTaskHelper.METADATA_FILE).toPath()
            format.write(metadata, outputFile)

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
