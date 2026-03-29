plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Security - CVE vulnerability scanning via OSV API"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    api(projects.components.shared.dependangerFeatureModel)
    implementation(projects.components.shared.dependangerHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.cvss.calculator)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
