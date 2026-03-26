package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.api.writeTomlTo
import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import org.gradle.api.tasks.TaskAction

public abstract class GenerateTomlTask : AbstractDependangerTask() {
    init {
        description = "Generate TOML version catalog from effective metadata"
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
        val tomlSettings = metadata.settings.toml
        val tomlConfig = TomlConfig(
            filename = tomlSettings.filename,
            includeComments = tomlSettings.includeComments,
            sortSections = tomlSettings.sortSections,
            useInlineVersions = tomlSettings.useInlineVersions,
            includeDeprecationComments = true,
        )

        val outputPath = outputDir.toPath()
        result.writeTomlTo(outputPath, tomlConfig)

        logger.lifecycle("Dependanger: Generated ${tomlConfig.filename} -> ${outputDir.resolve(tomlConfig.filename)}")
    }
}
