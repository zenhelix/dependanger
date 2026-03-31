package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.generators.bom.BomConfig
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
        extension.outputDirectory.file(BomConfig.DEFAULT_FILENAME)
    )

    @TaskAction
    public fun execute() {
        val effective = DependangerTaskHelper.readEffective(effectiveFile.get().asFile, logger)

        val groupId = project.group.toString().takeIf { it.isNotBlank() }
            ?: throw GradleException("BOM groupId not configured. Set project.group.")
        val artifactId = "${project.name}-bom"
        val version = project.version.toString().takeIf { it != "unspecified" && it.isNotBlank() }
            ?: throw GradleException("BOM version not configured. Set project.version.")

        val bomConfig = BomConfig(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            name = null,
            description = null,
            filename = BomConfig.DEFAULT_FILENAME,
            includeOptionalDependencies = false,
            prettyPrint = true,
            includeDeprecationComments = true,
        )

        val generator = BomGenerator(bomConfig)
        val artifact = generator.generate(effective)

        val outputFile = bomFile.get().asFile
        outputFile.parentFile?.mkdirs()
        generator.write(artifact, outputFile.parentFile.toPath())

        logger.lifecycle("Dependanger: Generated ${bomConfig.filename} -> $outputFile")
    }
}
