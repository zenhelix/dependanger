plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Security - CVE vulnerability scanning via OSV API"

dependencies {
    implementation(projects.components.core.dependangerCore)
    implementation(projects.components.core.dependangerEffective)
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(projects.components.shared.dependangerFeatureSupport)
    implementation(projects.components.shared.integrations.dependangerOsvHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
