package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.generators.bom.BomGenerator
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

public abstract class GenerateBomTask : AbstractDependangerTask() {
    init {
        description = "Generate Maven BOM from effective metadata"
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public val effectiveFile: RegularFileProperty = project.objects.fileProperty().convention(
        extension.outputDirectory.file(DependangerTaskHelper.EFFECTIVE_FILE)
    )

    @get:OutputFile
    public val bomFile: RegularFileProperty = project.objects.fileProperty().convention(
        extension.outputDirectory.file(extension.bom.filename)
    )

    @TaskAction
    public fun execute() {
        val effective = DependangerTaskHelper.readEffective(effectiveFile.get().asFile, logger)

        val fallbackGroupId = project.group.toString().takeIf { it.isNotBlank() }
            ?: throw GradleException("BOM groupId not configured. Set project.group or dependanger { bom { groupId.set(...) } }.")
        val fallbackArtifactId = "${project.name}-bom"
        val fallbackVersion = project.version.toString().takeIf { it != "unspecified" && it.isNotBlank() }
            ?: throw GradleException("BOM version not configured. Set project.version or dependanger { bom { version.set(...) } }.")

        val bomConfig = extension.bom.toConfig(fallbackGroupId, fallbackArtifactId, fallbackVersion)

        val generator = BomGenerator(bomConfig)
        val artifact = generator.generate(effective)

        val outputFile = bomFile.get().asFile
        outputFile.parentFile?.mkdirs()
        generator.write(artifact, outputFile.parentFile.toPath())

        logger.lifecycle("Dependanger: Generated ${bomConfig.filename} -> $outputFile")
    }
}
