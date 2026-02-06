package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class GenerateAllTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Run full generation pipeline"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
