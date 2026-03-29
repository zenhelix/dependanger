package io.github.zenhelix.dependanger.gradle

import org.gradle.api.tasks.TaskAction

public abstract class ListVersionsTask : AbstractDependangerTask() {
    init {
        description = "List all resolved versions from effective metadata"
    }

    @TaskAction
    public fun execute() {
        val outputDir = DependangerTaskHelper.ensureOutputDir(extension)
        val effective = DependangerTaskHelper.readEffective(outputDir, logger)

        val versions = effective.versions

        logger.lifecycle("Dependanger: Resolved versions (${versions.size}):")
        logger.lifecycle("")

        val maxAliasLen = versions.keys.maxOfOrNull { it.length } ?: 0
        versions.entries.sortedBy { it.key }.forEach { (alias, resolved) ->
            val paddedAlias = alias.padEnd(maxAliasLen)
            logger.lifecycle("  $paddedAlias = ${resolved.value}")
        }
    }
}
