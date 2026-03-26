package io.github.zenhelix.dependanger.gradle

import org.gradle.api.tasks.TaskAction

public abstract class GenerateAllTask : AbstractDependangerTask() {
    init {
        description = "Run full generation pipeline (metadata -> effective -> TOML + BOM)"
    }

    @TaskAction
    public fun execute() {
        val outputDir = extension.outputDirectory.get().asFile
        logger.lifecycle("Dependanger: Full generation complete -> $outputDir")
    }
}
