plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Maven Resolver - Maven repository integration"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    api(projects.components.shared.dependangerMavenPom)
    implementation(projects.components.shared.dependangerHttpClient)
    api(projects.components.shared.dependangerCache)
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
