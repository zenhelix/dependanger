package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import io.github.zenhelix.dependanger.generators.toml.TomlGenerator
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

public abstract class GenerateTomlTask : AbstractDependangerTask() {
    init {
        description = "Generate TOML version catalog from effective metadata"
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public val effectiveFile: RegularFileProperty = project.objects.fileProperty().convention(
        extension.outputDirectory.file(DependangerTaskHelper.EFFECTIVE_FILE)
    )

    @get:OutputFile
    public val tomlFile: RegularFileProperty = project.objects.fileProperty().convention(
        extension.outputDirectory.file(TomlConfig.DEFAULT_FILENAME)
    )

    @TaskAction
    public fun execute() {
        val effective = DependangerTaskHelper.readEffective(effectiveFile.get().asFile, logger)

        val generator = TomlGenerator(TomlConfig.DEFAULT)
        val artifact = generator.generate(effective)

        val outputFile = tomlFile.get().asFile
        outputFile.parentFile?.mkdirs()
        generator.write(artifact, outputFile.parentFile.toPath())

        logger.lifecycle("Dependanger: Generated ${TomlConfig.DEFAULT_FILENAME} -> $outputFile")
    }
}
