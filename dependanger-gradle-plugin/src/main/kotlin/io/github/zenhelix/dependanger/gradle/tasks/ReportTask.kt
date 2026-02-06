package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class ReportTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Generate dependency report"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
