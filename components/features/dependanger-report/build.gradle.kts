plugins {
    id("dependanger.feature")
}

description = "Dependanger Report - Dependency report generation"

dependencies {
    api(projects.components.core.dependangerCore)
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(libs.kaml)
    implementation(libs.commonmark)
}
