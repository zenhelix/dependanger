package io.github.zenhelix.dependanger.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DependangerPluginTest {

    private lateinit var project: Project

    companion object {
        private val ALL_TASK_NAMES = listOf(
            "dependangerGenerateMetadata",
            "dependangerGenerateEffective",
            "dependangerGenerateToml",
            "dependangerGenerateBom",
            "dependangerGenerate",
            "dependangerListVersions",
            "dependangerCheckUpdates",
            "dependangerAnalyze",
            "dependangerSecurityCheck",
            "dependangerLicenseCheck",
            "dependangerResolveTransitives",
            "dependangerValidate",
            "dependangerReport",
        )

        private val STANDALONE_TASK_NAMES = listOf(
            "dependangerCheckUpdates",
            "dependangerAnalyze",
            "dependangerSecurityCheck",
            "dependangerLicenseCheck",
            "dependangerResolveTransitives",
            "dependangerValidate",
            "dependangerReport",
        )
    }

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.zenhelix.dependanger")
    }

    @Test
    fun `plugin applies to project`() {
        val freshProject = ProjectBuilder.builder().build()
        assertDoesNotThrow {
            freshProject.plugins.apply("io.github.zenhelix.dependanger")
        }
    }

    @Test
    fun `extension has default values`() {
        val extension = project.extensions.getByType(DependangerExtension::class.java)

        assertNotNull(extension)
        assertTrue(
            extension.outputDirectory.get().asFile.path.endsWith("build/dependanger"),
            "outputDirectory should end with 'build/dependanger', but was: ${extension.outputDirectory.get().asFile.path}"
        )
        assertEquals(true, extension.failOnError.get())
    }

    @Test
    fun `all tasks registered with correct group`() {
        for (taskName in ALL_TASK_NAMES) {
            val task = project.tasks.getByName(taskName)
            assertEquals(
                "dependanger",
                task.group,
                "Task '$taskName' should have group 'dependanger'"
            )
        }
    }

    @Test
    fun `task descriptions not empty`() {
        for (taskName in ALL_TASK_NAMES) {
            val task = project.tasks.getByName(taskName)
            assertTrue(
                !task.description.isNullOrBlank(),
                "Task '$taskName' should have a non-empty description"
            )
        }
    }

    @Test
    fun `generative tasks have correct dependencies`() {
        assertTaskDependsOn("dependangerGenerateEffective", "dependangerGenerateMetadata")
        assertTaskDependsOn("dependangerGenerateToml", "dependangerGenerateEffective")
        assertTaskDependsOn("dependangerGenerateBom", "dependangerGenerateEffective")
        assertTaskDependsOn("dependangerGenerate", "dependangerGenerateToml")
        assertTaskDependsOn("dependangerGenerate", "dependangerGenerateBom")
        assertTaskDependsOn("dependangerListVersions", "dependangerGenerateEffective")
    }

    @Test
    fun `analysis tasks have no dependsOn`() {
        for (taskName in STANDALONE_TASK_NAMES) {
            val task = project.tasks.getByName(taskName)
            val dependencies = task.dependsOn
            assertTrue(
                dependencies.isEmpty(),
                "Task '$taskName' should have no dependencies, but has: $dependencies"
            )
        }
    }

    private fun assertTaskDependsOn(taskName: String, expectedDependency: String) {
        val task = project.tasks.getByName(taskName)
        val dependencyNames = task.taskDependencies.getDependencies(task).map { it.name }
        assertTrue(
            dependencyNames.contains(expectedDependency),
            "Task '$taskName' should depend on '$expectedDependency', but dependencies are: $dependencyNames"
        )
    }
}
