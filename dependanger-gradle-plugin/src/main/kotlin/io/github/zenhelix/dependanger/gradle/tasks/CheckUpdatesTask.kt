package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class CheckUpdatesTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Check for available updates"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
