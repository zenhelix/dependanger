package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public open class LicenseCheckTask : DefaultTask() {
    init {
        group = "dependanger"; description = "Check license compliance"
    }

    @TaskAction public fun execute(): Unit = TODO()
}
