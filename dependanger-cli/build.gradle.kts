plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    application
}

description = "Dependanger CLI - Command-line interface"

dependencies {
    implementation(projects.components.api.dependangerApi)
    implementation(projects.components.features.dependangerUpdates)
    implementation(projects.components.features.dependangerSecurity)
    implementation(projects.components.features.dependangerLicense)
    implementation(projects.components.features.dependangerTransitive)
    implementation(projects.components.features.dependangerAnalysis)
    implementation(projects.components.features.dependangerReport)
    implementation(libs.clikt)
    implementation(libs.mordant)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)
    implementation(libs.kotlin.logging.jvm)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass.set("io.github.zenhelix.dependanger.cli.DependangerCliKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.zenhelix.dependanger.cli.DependangerCliKt"
    }
}
