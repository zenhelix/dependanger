package io.github.zenhelix.dependanger.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public class DependangerPlugin : Plugin<Project> {

    public companion object {
        public const val TASK_GROUP: String = "dependanger"
        public const val EXTENSION_NAME: String = "dependanger"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, DependangerExtension::class.java, project)
        registerTasks(project, extension)
    }

    private fun registerTasks(project: Project, extension: DependangerExtension) {
        project.tasks.register("dependangerGenerateMetadata", GenerateMetadataTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerGenerateEffective", GenerateEffectiveTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerGenerateToml", GenerateTomlTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerGenerateBom", GenerateBomTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerGenerate", GenerateAllTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerValidate", ValidateTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerCheckUpdates", CheckUpdatesTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerAnalyze", AnalyzeTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerReport", ReportTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerListVersions", ListVersionsTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerSecurityCheck", SecurityCheckTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerLicenseCheck", LicenseCheckTask::class.java) { it.extension.set(extension) }
        project.tasks.register("dependangerResolveTransitives", ResolveTransitivesTask::class.java) { it.extension.set(extension) }
    }
}
