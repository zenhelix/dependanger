package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class AnalyzeTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Analyze compatibility"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
