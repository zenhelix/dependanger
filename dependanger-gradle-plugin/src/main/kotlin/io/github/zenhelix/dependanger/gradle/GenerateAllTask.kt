package io.github.zenhelix.dependanger.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

public abstract class GenerateAllTask : DefaultTask() {
    @get:Internal
    public abstract val extension: Property<DependangerExtension>

    init {
        group = DependangerPlugin.TASK_GROUP
        description = "Run full generation pipeline"
    }

    @TaskAction
    public fun execute(): Unit = TODO()
}
