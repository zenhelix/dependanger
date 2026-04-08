plugins {
    id("dependanger.feature")
}

description = "Dependanger Transitive - Transitive dependency resolution and constraints"

dependencies {
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(projects.components.shared.dependangerFeatureSupport)
    implementation(projects.components.shared.integrations.dependangerMavenHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
}
