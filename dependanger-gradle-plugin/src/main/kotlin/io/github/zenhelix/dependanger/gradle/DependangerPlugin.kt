package io.github.zenhelix.dependanger.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public class DependangerPlugin : Plugin<Project> {

    public companion object {
        public const val TASK_GROUP: String = "dependanger"
        public const val EXTENSION_NAME: String = "dependanger"
    }

    override fun apply(project: Project) {
        project.extensions.create(EXTENSION_NAME, DependangerExtension::class.java, project)
        registerTasks(project)
    }

    private fun registerTasks(project: Project) {
        // Category 1: Generative tasks (file chain)
        val generateMetadata = project.tasks.register("dependangerGenerateMetadata", GenerateMetadataTask::class.java)
        val generateEffective = project.tasks.register("dependangerGenerateEffective", GenerateEffectiveTask::class.java) {
            it.dependsOn(generateMetadata)
        }
        val generateToml = project.tasks.register("dependangerGenerateToml", GenerateTomlTask::class.java) {
            it.dependsOn(generateEffective)
        }
        val generateBom = project.tasks.register("dependangerGenerateBom", GenerateBomTask::class.java) {
            it.dependsOn(generateEffective)
        }
        project.tasks.register("dependangerGenerate", GenerateAllTask::class.java) {
            it.dependsOn(generateToml, generateBom)
        }
        project.tasks.register("dependangerListVersions", ListVersionsTask::class.java) {
            it.dependsOn(generateEffective)
        }

        // Category 2: Analytical tasks (standalone, no dependsOn)
        project.tasks.register("dependangerCheckUpdates", CheckUpdatesTask::class.java)
        project.tasks.register("dependangerAnalyze", AnalyzeTask::class.java)
        project.tasks.register("dependangerSecurityCheck", SecurityCheckTask::class.java)
        project.tasks.register("dependangerLicenseCheck", LicenseCheckTask::class.java)
        project.tasks.register("dependangerResolveTransitives", ResolveTransitivesTask::class.java)

        // Special tasks (standalone)
        project.tasks.register("dependangerValidate", ValidateTask::class.java)
        project.tasks.register("dependangerReport", ReportTask::class.java)
    }
}
