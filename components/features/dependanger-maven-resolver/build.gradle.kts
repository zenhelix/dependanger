plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Maven Resolver - BOM import resolution processor"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    implementation(projects.components.shared.integrations.dependangerMavenHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
