plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Report - Dependency report generation"

dependencies {
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(projects.components.core.dependangerCore)
    implementation(projects.components.core.dependangerEffective)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)
    implementation(libs.commonmark)
    implementation(libs.kotlin.logging.jvm)

    testRuntimeOnly(libs.slf4j.simple)
}
