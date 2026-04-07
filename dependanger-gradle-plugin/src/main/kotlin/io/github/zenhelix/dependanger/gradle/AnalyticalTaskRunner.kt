package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.DependangerBuilder
import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.core.exception.DependangerException
import kotlinx.coroutines.runBlocking
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger

internal class AnalyticalTaskRunner(
    private val extension: DependangerExtension,
    private val logger: Logger,
) {
    fun run(
        checkSuccess: Boolean = true,
        configure: DependangerBuilder.() -> Unit = {},
        handle: AnalyticalTaskContext.(DependangerResult) -> Unit,
    ) {
        val metadata = extension.toMetadata()
        val failOnError = extension.failOnError.get()

        try {
            val dependanger = Dependanger.fromMetadata(metadata)
                .apply(configure)
                .build()

            val result = runBlocking { dependanger.process() }

            if (checkSuccess && !result.isSuccess) {
                DependangerTaskHelper.handleProcessingErrors(result, failOnError, logger)
                return
            }

            AnalyticalTaskContext(logger, failOnError).handle(result)
        } catch (e: DependangerException) {
            if (failOnError) {
                throw GradleException("Dependanger: ${e.message}", e)
            } else {
                logger.warn("Dependanger: ${e.message}")
            }
        }
    }
}

internal class AnalyticalTaskContext(
    val logger: Logger,
    val failOnError: Boolean,
)
