package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class GenerateBomTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Generate Maven BOM"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
