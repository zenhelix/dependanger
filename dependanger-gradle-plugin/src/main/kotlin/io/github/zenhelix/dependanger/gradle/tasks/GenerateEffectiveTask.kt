package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class GenerateEffectiveTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Generate effective metadata from metadata.json"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
