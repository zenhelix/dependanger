@file:Suppress("UnstableApiUsage")

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
    `jvm-test-suite`
}

description = "Dependanger Gradle Plugin - Gradle integration"

dependencies {
    // Single dependency on dependanger-api facade - provides all core functionality
    implementation(project(":components:api:dependanger-api"))

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
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
                implementation("org.assertj:assertj-core:3.27.3")
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
