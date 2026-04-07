plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger License - License compliance checking via Maven POM / ClearlyDefined API"

dependencies {
    implementation(projects.components.core.dependangerCore)
    implementation(projects.components.core.dependangerEffective)
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(projects.components.shared.dependangerFeatureSupport)
    implementation(projects.components.shared.integrations.dependangerMavenHttpClient)
    implementation(projects.components.shared.integrations.dependangerClearlydefinedHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
