package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class GenerateTomlTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Generate TOML version catalog"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
