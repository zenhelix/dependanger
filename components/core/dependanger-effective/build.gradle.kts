plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Effective - Filtered and enriched metadata model"

dependencies {
    api(projects.components.core.dependangerCore)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.slf4j.simple)
}
