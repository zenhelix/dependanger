package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.transitives
import io.github.zenhelix.dependanger.api.versionConflicts
import io.github.zenhelix.dependanger.features.transitive.TransitiveResolverProcessor
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.TaskAction

public abstract class ResolveTransitivesTask : AbstractDependangerTask() {
    init {
        description = "Resolve transitive dependencies and detect version conflicts"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.dsl.toMetadata()
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            val dependanger = Dependanger.fromMetadata(metadata)
                .configureProcessing { enableOptional(TransitiveResolverProcessor.PROCESSOR_ID) }
                .build()

            val result = runBlocking { dependanger.process() }

            if (!result.isSuccess) {
                DependangerTaskHelper.handleProcessingErrors(result, failOnError, logger)
                return@runWithErrorHandling
            }

            val trees = result.transitives
            val conflicts = result.versionConflicts

            logger.lifecycle("Dependanger: Resolved transitive dependencies:")
            logger.lifecycle("  Direct dependencies: ${result.effectiveOrNull()?.libraries?.size ?: 0}")
            logger.lifecycle("  Dependency trees: ${trees.size}")
            logger.lifecycle("  Version conflicts: ${conflicts.size}")

            if (conflicts.isNotEmpty()) {
                logger.lifecycle("")
                logger.lifecycle("  Conflicts:")
                conflicts.forEach { conflict ->
                    logger.warn("    ${conflict.group}:${conflict.artifact}: ${conflict.requestedVersions.joinToString(" vs ")} (resolved: ${conflict.resolvedVersion})")
                }
            }
        }
    }
}
