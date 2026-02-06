package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class ValidateTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Validate DSL configuration"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
