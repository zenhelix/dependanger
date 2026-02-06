package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class SecurityCheckTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Check for security vulnerabilities"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
