package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

public abstract class ReportTask : DefaultTask() {
    @get:Internal
    public abstract val extension: Property<DependangerExtension>

    init {
        group = DependangerPlugin.TASK_GROUP
        description = "Generate dependency report"
    }

    @TaskAction
    public fun execute(): Unit = TODO()
}
