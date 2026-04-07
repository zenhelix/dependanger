package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.vulnerabilities
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

public abstract class SecurityCheckTask : AbstractDependangerTask() {
    init {
        description = "Check dependencies for known security vulnerabilities"
    }

    @TaskAction
    public fun execute(): Unit = AnalyticalTaskRunner(extension, logger).run(
        configure = { configureProcessing { enableOptional(FeatureProcessorIds.SECURITY_CHECK) } },
        handle = { result ->
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
    )
}
