package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.vulnerabilities
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitySeverity
import kotlinx.coroutines.runBlocking
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

public abstract class SecurityCheckTask : AbstractDependangerTask() {
    init {
        description = "Check dependencies for known security vulnerabilities"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.dsl.toMetadata()
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata)
                .configureProcessing { enableOptional(ProcessorIds.SECURITY_CHECK) }
                .build()

            val result = runBlocking { dependanger.process() }

            if (!result.isSuccess) {
                DependangerTaskHelper.handleProcessingErrors(result, failOnError, logger)
                return@runWithErrorHandling
            }

            val vulnerabilities = result.vulnerabilities

            if (vulnerabilities.isEmpty()) {
                logger.lifecycle("Dependanger: No known vulnerabilities found.")
            } else {
                logger.lifecycle("Dependanger: Found ${vulnerabilities.size} vulnerabilities:")
                vulnerabilities.forEach { vuln ->
                    logger.warn("  ${vuln.affectedGroup}:${vuln.affectedArtifact}:${vuln.affectedVersion} (${vuln.id}): ${vuln.severity} - ${vuln.summary}")
                }
            }

            val critical = vulnerabilities.filter { it.severity.meetsThreshold(VulnerabilitySeverity.HIGH) }
            if (critical.isNotEmpty() && failOnError) {
                throw GradleException("Dependanger: Found ${critical.size} vulnerabilities with HIGH or CRITICAL severity.")
            }
        }
    }
}
