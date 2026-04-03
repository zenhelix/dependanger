@file:Suppress("UnstableApiUsage")

plugins {
    id("dependanger.base")
    `java-gradle-plugin`
    alias(libs.plugins.gradle.plugin.publish)
    `jvm-test-suite`
}

description = "Dependanger Gradle Plugin - Gradle integration"

dependencies {
    implementation(projects.components.api.dependangerApi)
    // Generators and metadata-json are declared directly because the plugin uses their types
    // (TomlConfig, BomConfig, TomlGenerator, BomGenerator, JsonSerializationFormat)
    // for task configuration and file-based pipeline I/O.
    // These are implementation-scoped in dependanger-api, so not available transitively.
    implementation(projects.components.generators.dependangerGeneratorToml)
    implementation(projects.components.generators.dependangerGeneratorBom)
    implementation(projects.components.core.dependangerMetadataJson)

    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    website = "https://github.com/zenhelix/dependanger"
    vcsUrl = "https://github.com/zenhelix/dependanger.git"

    plugins {
        create("dependanger") {
            id = "io.github.zenhelix.dependanger"
            implementationClass = "io.github.zenhelix.dependanger.gradle.DependangerPlugin"
            displayName = "Dependanger - Dependency Management Plugin"
            description = "A Gradle plugin for managing dependencies with Version Catalogs and BOMs generation"
            tags.set(listOf("dependencies", "version-catalog", "bom", "kotlin", "multiplatform"))
        }
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()

            dependencies {
                implementation(project())
                implementation(gradleTestKit())
                implementation(libs.assertj.core)
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

val functionalTest by sourceSets.existing
gradlePlugin.testSourceSets(functionalTest.get())

tasks.named("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

publishing {
    repositories {
        mavenLocal()
    }
}
