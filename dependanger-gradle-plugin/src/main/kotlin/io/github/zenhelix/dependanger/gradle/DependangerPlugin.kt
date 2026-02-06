package io.github.zenhelix.dependanger.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public class DependangerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dependanger", DependangerExtension::class.java, project)
        registerTasks(project, extension)
    }

    private fun registerTasks(project: Project, extension: DependangerExtension) {
        project.tasks.register("dependangerGenerateMetadata", GenerateMetadataTask::class.java)
        project.tasks.register("dependangerGenerateEffective", GenerateEffectiveTask::class.java)
        project.tasks.register("dependangerGenerateToml", GenerateTomlTask::class.java)
        project.tasks.register("dependangerGenerateBom", GenerateBomTask::class.java)
        project.tasks.register("dependangerGenerate", GenerateAllTask::class.java)
        project.tasks.register("dependangerValidate", ValidateTask::class.java)
        project.tasks.register("dependangerProcess", ProcessTask::class.java)
        project.tasks.register("dependangerCheckUpdates", CheckUpdatesTask::class.java)
        project.tasks.register("dependangerAnalyze", AnalyzeTask::class.java)
        project.tasks.register("dependangerReport", ReportTask::class.java)
        project.tasks.register("dependangerListVersions", ListVersionsTask::class.java)
        project.tasks.register("dependangerSecurityCheck", SecurityCheckTask::class.java)
        project.tasks.register("dependangerLicenseCheck", LicenseCheckTask::class.java)
        project.tasks.register("dependangerResolveTransitives", ResolveTransitivesTask::class.java)
    }
}
