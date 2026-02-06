import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.1.20" apply false
    kotlin("plugin.serialization") version "2.1.20" apply false
    id("com.gradle.plugin-publish") version "2.0.0" apply false
    id("org.jetbrains.dokka") version "2.0.0" apply false
    `jacoco-report-aggregation`
}

val jdkVersion = JavaVersion.VERSION_17
val kotlinJvmTarget = JvmTarget.fromTarget(jdkVersion.toString())
val kotlinLanguageVersion = KotlinVersion.KOTLIN_2_1

allprojects {
    group = "io.github.zenhelix"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")

    configure<JavaPluginExtension> {
        sourceCompatibility = jdkVersion
        targetCompatibility = jdkVersion
        withJavadocJar()
        withSourcesJar()
    }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        explicitApi()

        compilerOptions {
            jvmTarget.set(kotlinJvmTarget)
            languageVersion.set(kotlinLanguageVersion)
            apiVersion.set(kotlinLanguageVersion)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-Xcontext-receivers"
            )
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    dependencies {
        val testImplementation by configurations
        val testRuntimeOnly by configurations

        testImplementation(platform("org.junit:junit-bom:5.11.4"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("org.assertj:assertj-core:3.27.3")
        testImplementation("io.mockk:mockk:1.13.16")
    }
}

dependencies {
    subprojects.forEach { subproject ->
        jacocoAggregation(subproject)
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Build all modules"
    dependsOn(subprojects.map { it.tasks.named("build") })
}

tasks.register("testAll") {
    group = "verification"
    description = "Run tests in all modules"
    dependsOn(subprojects.map { it.tasks.named("test") })
}
