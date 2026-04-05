plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Transitive - Transitive dependency resolution and constraints"

dependencies {
    implementation(projects.components.core.dependangerCore)
    implementation(projects.components.core.dependangerEffective)
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(projects.components.shared.integrations.dependangerMavenHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
