plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Cache - Shared file-based caching infrastructure"

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    implementation(libs.kotlin.logging.jvm)

    testRuntimeOnly(libs.slf4j.simple)
}
