plugins {
    id("dependanger.feature")
}

description = "Dependanger Updates - Version update checker"

dependencies {
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(projects.components.shared.dependangerFeatureSupport)
    implementation(projects.components.shared.integrations.dependangerMavenHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlinx.datetime)
}
