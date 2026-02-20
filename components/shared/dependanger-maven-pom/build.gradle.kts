plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Maven POM - Shared Maven POM parsing and writing"

dependencies {
    api(libs.kotlinx.serialization.core)
    testRuntimeOnly(libs.slf4j.simple)
}
