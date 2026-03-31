plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Maven Resolver - BOM import resolution processor"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    api(projects.components.shared.dependangerMavenClient)
    api(projects.components.shared.dependangerCache)
    implementation(projects.components.shared.dependangerHttpClient)
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
