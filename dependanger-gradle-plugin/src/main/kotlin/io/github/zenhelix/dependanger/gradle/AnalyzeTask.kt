package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import org.gradle.api.tasks.TaskAction

public abstract class AnalyzeTask : AbstractDependangerTask() {
    init {
        description = "Analyze dependency compatibility"
    }

    @TaskAction
    public fun execute(): Unit = AnalyticalTaskRunner(extension, logger).run(
        checkSuccess = false,
        configure = {
            jdkVersion(Runtime.version().feature())
            configureProcessing { enableOptional(FeatureProcessorIds.COMPATIBILITY_ANALYSIS) }
        },
        handle = { result ->
            val compatPredicate = { msg: DiagnosticMessage -> msg.code.startsWith("COMPAT") }

            val hadIssues = DependangerTaskHelper.handleFilteredDiagnostics(
                result, failOnError, logger,
                predicate = compatPredicate,
                summaryMessage = { e, w -> "Dependanger: Analysis complete: $e errors, $w warnings" },
                errorMessage = { count -> "Dependanger: Compatibility analysis found $count error(s)." },
            )

            if (!hadIssues) {
                logger.lifecycle("Dependanger: No compatibility issues found.")
            }
        }
    )
}
