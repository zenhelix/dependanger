plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Report - Dependency report generation"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    implementation(projects.components.features.dependangerUpdates)
    implementation(projects.components.features.dependangerSecurity)
    implementation(projects.components.features.dependangerLicense)
    implementation(projects.components.features.dependangerTransitive)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)
    implementation(libs.commonmark)
    implementation(libs.kotlin.logging.jvm)
}
