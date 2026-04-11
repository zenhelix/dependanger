plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger OSV Client - OpenSSF OSV API client for vulnerability scanning"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.shared.dependangerHttpClient)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.cvss.calculator)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
