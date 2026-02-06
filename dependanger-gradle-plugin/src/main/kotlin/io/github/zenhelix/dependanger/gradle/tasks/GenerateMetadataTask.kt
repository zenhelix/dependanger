package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class GenerateMetadataTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Generate metadata.json from DSL"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
