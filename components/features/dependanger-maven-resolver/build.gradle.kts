plugins {
    id("dependanger.feature")
}

description = "Dependanger Maven Resolver - BOM import resolution processor"

dependencies {
    implementation(projects.components.shared.integrations.dependangerMavenHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlinx.datetime)
}
