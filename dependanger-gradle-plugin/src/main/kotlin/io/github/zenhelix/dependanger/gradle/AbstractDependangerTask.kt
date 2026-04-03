package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.core.exception.DependangerException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal

public abstract class AbstractDependangerTask : DefaultTask() {
    init {
        group = DependangerPlugin.TASK_GROUP
    }

    @get:Internal
    public val extension: DependangerExtension
        get() = project.extensions.getByType(DependangerExtension::class.java)

    protected inline fun runWithErrorHandling(
        failOnError: Boolean,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: DependangerException) {
            if (failOnError) {
                throw GradleException("Dependanger: ${e.message}", e)
            } else {
                logger.warn("Dependanger: ${e.message}")
            }
        }
    }
}
