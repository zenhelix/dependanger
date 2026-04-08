plugins {
    id("dependanger.feature")
}

description = "Dependanger Report - Dependency report generation"

dependencies {
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(libs.kaml)
    implementation(libs.commonmark)
}
