plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Report - Dependency report generation"

dependencies {
    api(projects.components.shared.dependangerFeatureModel)
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)
    implementation(libs.commonmark)
    implementation(libs.kotlin.logging.jvm)

    testRuntimeOnly(libs.slf4j.simple)
}
