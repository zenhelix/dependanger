package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class ListVersionsTask : DefaultTask() {
    init {
        group = "dependanger"; description = "List all versions"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
