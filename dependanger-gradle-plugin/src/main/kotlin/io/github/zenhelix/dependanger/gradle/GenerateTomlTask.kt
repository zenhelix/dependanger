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
        val effective = DependangerTaskHelper.readEffective(outputDir, logger)

        val result = DependangerResult(
            effective = effective,
            diagnostics = effective.diagnostics,
        )

        val tomlConfig = TomlConfig.DEFAULT

        val outputPath = outputDir.toPath()
        result.writeTomlTo(outputPath, tomlConfig)

        logger.lifecycle("Dependanger: Generated ${tomlConfig.filename} -> ${outputDir.resolve(tomlConfig.filename)}")
    }
}
