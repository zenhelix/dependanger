package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.TaskAction

public abstract class AnalyzeTask : AbstractDependangerTask() {
    init {
        description = "Analyze dependency compatibility"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.toMetadata()
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata)
                .jdkVersion(Runtime.version().feature())
                .configureProcessing { enableOptional(FeatureProcessorIds.COMPATIBILITY_ANALYSIS) }
                .build()

            val result = runBlocking { dependanger.process() }

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
    }
}
