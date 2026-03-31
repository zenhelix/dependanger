package io.github.zenhelix.dependanger.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DependangerPluginFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private val buildFile get() = projectDir.resolve("build.gradle.kts")
    private val settingsFile get() = projectDir.resolve("settings.gradle.kts")

    @BeforeEach
    fun setUp() {
        settingsFile.writeText("""rootProject.name = "test-project"""")
    }

    private fun runTask(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(*args, "--stacktrace")
        .withPluginClasspath()
        .build()

    private fun runTaskAndFail(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(*args, "--stacktrace")
        .withPluginClasspath()
        .buildAndFail()

    @Test
    fun `empty DSL generates empty metadata`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger { }
        """.trimIndent()
        )

        val result = runTask("dependangerGenerateMetadata")

        assertThat(result.task(":dependangerGenerateMetadata")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val metadataFile = projectDir.resolve("build/dependanger/metadata.json")
        assertThat(metadataFile).exists()
        val content = metadataFile.readText()
        assertThat(content).contains("\"schemaVersion\"")
        assertThat(content).contains("\"versions\"")
    }

    @Test
    fun `DSL with versions generates metadata`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                versions {
                    version("kotlin", "2.1.20")
                    version("junit", "5.11.0")
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerGenerateMetadata")

        assertThat(result.task(":dependangerGenerateMetadata")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val content = projectDir.resolve("build/dependanger/metadata.json").readText()
        assertThat(content).contains("kotlin")
        assertThat(content).contains("2.1.20")
        assertThat(content).contains("junit")
        assertThat(content).contains("5.11.0")
    }

    @Test
    fun `generateEffective creates effective json`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                versions {
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerGenerateEffective")

        assertThat(result.task(":dependangerGenerateMetadata")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":dependangerGenerateEffective")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/dependanger/metadata.json")).exists()
        assertThat(projectDir.resolve("build/dependanger/effective.json")).exists()
    }

    @Test
    fun `generateToml creates TOML file`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                versions {
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerGenerateToml")

        assertThat(result.task(":dependangerGenerateToml")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val tomlFile = projectDir.resolve("build/dependanger/libs.versions.toml")
        assertThat(tomlFile).exists()
        val content = tomlFile.readText()
        assertThat(content).contains("[versions]")
        assertThat(content).contains("[libraries]")
        assertThat(content).contains("kotlin")
    }

    @Test
    fun `generateBom creates BOM file`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            group = "com.example"
            version = "1.0.0"
            dependanger {
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerGenerateBom")

        assertThat(result.task(":dependangerGenerateBom")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val bomFile = projectDir.resolve("build/dependanger/bom.pom.xml")
        assertThat(bomFile).exists()
        val content = bomFile.readText()
        assertThat(content).contains("com.example")
        assertThat(content).contains("test-project-bom")
    }

    @Test
    fun `generate runs full pipeline`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            group = "com.example"
            version = "1.0.0"
            dependanger {
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerGenerate")

        assertThat(result.task(":dependangerGenerate")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/dependanger/libs.versions.toml")).exists()
        assertThat(projectDir.resolve("build/dependanger/bom.pom.xml")).exists()
    }

    @Test
    fun `validate passes for valid config`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                versions {
                    version("kotlin", "2.1.20")
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerValidate")

        assertThat(result.task(":dependangerValidate")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("Validation completed")
    }

    @Test
    fun `outputDirectory customization`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                outputDirectory.set(layout.buildDirectory.dir("custom-output"))
                versions {
                    version("kotlin", "2.1.20")
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerGenerateMetadata")

        assertThat(result.task(":dependangerGenerateMetadata")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/custom-output/metadata.json")).exists()
    }

    @Test
    fun `task dependencies work correctly`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerGenerateToml")

        assertThat(result.task(":dependangerGenerateMetadata")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":dependangerGenerateEffective")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":dependangerGenerateToml")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `listVersions displays resolved versions`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                versions {
                    version("kotlin", "2.1.20")
                    version("junit", "5.11.0")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerListVersions")

        assertThat(result.task(":dependangerListVersions")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("Resolved versions")
    }

    @Test
    fun `full workflow end-to-end`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            group = "com.example"
            version = "1.0.0"
            dependanger {
                versions {
                    version("kotlin", "2.1.20")
                    version("spring", "3.4.1")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                    library("spring-web", "org.springframework:spring-web:3.4.1")
                }
                bundles {
                    bundle("core") {
                        libraries("kotlin-stdlib", "spring-web")
                    }
                }
            }
        """.trimIndent()
        )

        val result = runTask("dependangerGenerate")

        assertThat(result.task(":dependangerGenerate")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val metadata = projectDir.resolve("build/dependanger/metadata.json")
        assertThat(metadata).exists()
        assertThat(metadata.readText()).contains("kotlin-stdlib")

        assertThat(projectDir.resolve("build/dependanger/effective.json")).exists()

        val toml = projectDir.resolve("build/dependanger/libs.versions.toml")
        assertThat(toml).exists()
        assertThat(toml.readText()).contains("[versions]")
        assertThat(toml.readText()).contains("[libraries]")

        val bom = projectDir.resolve("build/dependanger/bom.pom.xml")
        assertThat(bom).exists()
        assertThat(bom.readText()).contains("com.example")
    }

    @Test
    fun `generateEffective reads from metadata json not from DSL directly`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                versions {
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                }
            }
        """.trimIndent()
        )

        // Step 1: Generate metadata.json
        runTask("dependangerGenerateMetadata")
        val metadataFile = projectDir.resolve("build/dependanger/metadata.json")
        assertThat(metadataFile).exists()

        // Step 2: Modify metadata.json to inject a marker version that does NOT exist in DSL
        val originalContent = metadataFile.readText()
        assertThat(originalContent).contains("kotlin")
        val modifiedContent = originalContent.replace("2.1.20", "9.9.99-marker")
        metadataFile.writeText(modifiedContent)

        // Step 3: Run generateEffective (skipping generateMetadata via -x)
        // If generateEffective reads from the file, effective.json will contain the marker version.
        // If it re-evaluates the DSL, effective.json will contain the original 2.1.20.
        val result = runTask("dependangerGenerateEffective", "-x", "dependangerGenerateMetadata")

        assertThat(result.task(":dependangerGenerateEffective")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val effectiveContent = projectDir.resolve("build/dependanger/effective.json").readText()
        assertThat(effectiveContent)
            .describedAs("GenerateEffectiveTask should read from metadata.json, not re-evaluate DSL")
            .contains("9.9.99-marker")
        assertThat(effectiveContent).doesNotContain("2.1.20")
    }

    @Test
    fun `generateEffective fails when metadata json is missing`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                versions {
                    version("kotlin", "2.1.20")
                }
            }
        """.trimIndent()
        )

        // Run generateEffective without generateMetadata — metadata.json does not exist
        // Gradle validates @InputFile and fails before the task action executes
        val result = runTaskAndFail("dependangerGenerateEffective", "-x", "dependangerGenerateMetadata")

        assertThat(result.output).contains("metadata.json")
        assertThat(result.output).contains("doesn't exist")
    }

    @Test
    fun `downstream tasks are UP-TO-DATE on second run when inputs unchanged`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.zenhelix.dependanger")
            }
            dependanger {
                versions {
                    version("kotlin", "2.1.20")
                }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
                }
            }
        """.trimIndent()
        )

        // First run — all tasks execute
        val firstRun = runTask("dependangerGenerateToml")
        assertThat(firstRun.task(":dependangerGenerateMetadata")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(firstRun.task(":dependangerGenerateEffective")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(firstRun.task(":dependangerGenerateToml")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Second run — GenerateMetadata always runs (DSL not trackable),
        // but Effective and Toml should be UP-TO-DATE since metadata.json content didn't change
        val secondRun = runTask("dependangerGenerateToml")
        assertThat(secondRun.task(":dependangerGenerateMetadata")?.outcome)
            .describedAs("GenerateMetadata always re-executes (DSL is not trackable)")
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(secondRun.task(":dependangerGenerateEffective")?.outcome)
            .describedAs("GenerateEffective should be UP-TO-DATE when metadata.json unchanged")
            .isEqualTo(TaskOutcome.UP_TO_DATE)
        assertThat(secondRun.task(":dependangerGenerateToml")?.outcome)
            .describedAs("GenerateToml should be UP-TO-DATE when effective.json unchanged")
            .isEqualTo(TaskOutcome.UP_TO_DATE)
    }
}
