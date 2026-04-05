plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger ClearlyDefined Client - License information via ClearlyDefined API"

dependencies {
    api(projects.components.shared.dependangerHttpClient)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
