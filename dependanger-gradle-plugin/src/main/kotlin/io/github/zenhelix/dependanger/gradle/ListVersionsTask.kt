package io.github.zenhelix.dependanger.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

public abstract class ListVersionsTask : AbstractDependangerTask() {
    init {
        description = "List all resolved versions from effective metadata"
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public val effectiveFile: RegularFileProperty = project.objects.fileProperty().convention(
        extension.outputDirectory.file(DependangerTaskHelper.EFFECTIVE_FILE)
    )

    @TaskAction
    public fun execute() {
        val effective = DependangerTaskHelper.readEffective(effectiveFile.get().asFile, logger)

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
