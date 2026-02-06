package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class ProcessTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Run processing pipeline"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
