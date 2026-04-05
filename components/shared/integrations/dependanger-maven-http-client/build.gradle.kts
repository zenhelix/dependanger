plugins {
    id("dependanger.base")
    id("dependanger.publishing")
}

description = "Dependanger Maven Client - Maven POM downloading and parsing utilities"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.shared.dependangerMavenPom)
    api(projects.components.shared.dependangerHttpClient)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
