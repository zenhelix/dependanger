package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.transitives
import io.github.zenhelix.dependanger.api.versionConflicts
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import org.gradle.api.tasks.TaskAction

public abstract class ResolveTransitivesTask : AbstractDependangerTask() {
    init {
        description = "Resolve transitive dependencies and detect version conflicts"
    }

    @TaskAction
    public fun execute(): Unit = AnalyticalTaskRunner(extension, logger).run(
        configure = { configureProcessing { enableOptional(FeatureProcessorIds.TRANSITIVE_RESOLVER) } },
        handle = { result ->
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
    )
}
