package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.licenseViolations
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationType
import kotlinx.coroutines.runBlocking
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

public abstract class LicenseCheckTask : AbstractDependangerTask() {
    init {
        description = "Check dependency license compliance"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.dsl.toMetadata()
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata)
                .configureProcessing { enableOptional(ProcessorIds.LICENSE_CHECK) }
                .build()

            val result = runBlocking { dependanger.process() }

            if (!result.isSuccess) {
                DependangerTaskHelper.handleProcessingErrors(result, failOnError, logger)
                return@runWithErrorHandling
            }

            val violations = result.licenseViolations
            val denied = violations.filter { it.violationType == LicenseViolationType.DENIED }
            val notAllowed = violations.filter { it.violationType == LicenseViolationType.NOT_ALLOWED }

            if (denied.isEmpty() && notAllowed.isEmpty()) {
                logger.lifecycle("Dependanger: All library licenses are compliant.")
            } else {
                denied.forEach { logger.error("Dependanger LICENSE DENIED: ${it.group}:${it.artifact} - ${it.detectedLicense ?: "unknown"} (${it.message})") }
                notAllowed.forEach { logger.warn("Dependanger LICENSE NOT_ALLOWED: ${it.group}:${it.artifact} - ${it.detectedLicense ?: "unknown"} (${it.message})") }
            }

            if (denied.isNotEmpty() && failOnError) {
                throw GradleException("Dependanger: Found ${denied.size} denied license(s).")
            }
        }
    }
}
