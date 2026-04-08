plugins {
    id("dependanger.feature")
}

description = "Dependanger License - License compliance checking via Maven POM / ClearlyDefined API"

dependencies {
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(projects.components.shared.dependangerFeatureSupport)
    implementation(projects.components.shared.integrations.dependangerMavenHttpClient)
    implementation(projects.components.shared.integrations.dependangerClearlydefinedHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlinx.datetime)
}
