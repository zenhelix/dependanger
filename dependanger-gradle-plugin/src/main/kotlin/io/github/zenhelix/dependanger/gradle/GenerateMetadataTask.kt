package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

public abstract class GenerateMetadataTask : AbstractDependangerTask() {
    init {
        description = "Generate metadata.json from DSL"
    }

    @get:Input
    public val metadataHash: Provider<String> = project.provider {
        val json = JsonSerializationFormat().serialize(extension.dsl.toMetadata())
        MessageDigest.getInstance("SHA-256")
            .digest(json.toByteArray())
            .joinToString("") { "%02x".format(it) }
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
            JsonSerializationFormat().write(metadata, outputFile.toPath())

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
