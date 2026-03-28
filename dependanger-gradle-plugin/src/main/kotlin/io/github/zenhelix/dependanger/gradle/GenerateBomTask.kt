package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.api.writeBomTo
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

public abstract class GenerateBomTask : AbstractDependangerTask() {
    init {
        description = "Generate Maven BOM from effective metadata"
    }

    @TaskAction
    public fun execute() {
        val outputDir = DependangerTaskHelper.ensureOutputDir(extension)
        val effective = DependangerTaskHelper.readEffective(outputDir)

        val result = DependangerResult(
            effective = effective,
            diagnostics = effective.diagnostics,
        )

        val metadata = extension.dsl.toMetadata()
        val bomSettings = metadata.settings.bom
        val groupId = bomSettings.groupId
            ?: project.group.toString().takeIf { it.isNotBlank() }
            ?: throw GradleException("BOM groupId not configured. Set it in dependanger { settings { bom { groupId = \"...\" } } } or set project.group.")
        val artifactId = bomSettings.artifactId ?: "${project.name}-bom"
        val version = bomSettings.version
            ?: project.version.toString().takeIf { it != "unspecified" && it.isNotBlank() }
            ?: throw GradleException("BOM version not configured. Set it in dependanger { settings { bom { version = \"...\" } } } or set project.version.")

        val bomConfig = BomConfig(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            name = null,
            description = null,
            filename = BomConfig.DEFAULT_FILENAME,
            includeOptionalDependencies = bomSettings.includeOptionalDependencies,
            prettyPrint = true,
            includeDeprecationComments = true,
        )

        result.writeBomTo(outputDir.toPath(), bomConfig)

        logger.lifecycle("Dependanger: Generated ${bomConfig.filename} -> ${outputDir.resolve(bomConfig.filename)}")
    }
}
