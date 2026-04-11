package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.licenseViolations
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationType
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

public abstract class LicenseCheckTask : AbstractDependangerTask() {
    init {
        description = "Check dependency license compliance"
    }

    @TaskAction
    public fun execute(): Unit = AnalyticalTaskRunner(extension, logger).run(
        configure = { configureProcessing { enableOptional(FeatureProcessorIds.LICENSE_CHECK) } },
        handle = { result ->
            val violations = result.licenseViolations
            val denied = violations.filter { it.violationType == LicenseViolationType.DENIED }
            val notAllowed = violations.filter { it.violationType == LicenseViolationType.NOT_ALLOWED }

            if (denied.isEmpty() && notAllowed.isEmpty()) {
                logger.lifecycle("Dependanger: All library licenses are compliant.")
            } else {
                denied.forEach { logger.error("Dependanger LICENSE DENIED: ${it.coordinate} - ${it.detectedLicense ?: "unknown"} (${it.message})") }
                notAllowed.forEach { logger.warn("Dependanger LICENSE NOT_ALLOWED: ${it.coordinate} - ${it.detectedLicense ?: "unknown"} (${it.message})") }
            }

            if (denied.isNotEmpty() && failOnError) {
                throw GradleException("Dependanger: Found ${denied.size} denied license(s).")
            }
        }
    )
}
