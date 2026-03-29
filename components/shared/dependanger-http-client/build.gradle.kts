plugins {
    id("dependanger.base")
    id("dependanger.publishing")
}

description = "Dependanger HTTP Client - Shared HTTP client infrastructure with retry and error handling"

dependencies {
    api(projects.components.core.dependangerCore)
    api(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
