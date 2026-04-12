plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Maven POM - Shared Maven POM parsing and writing"

dependencies {
    api(libs.kotlinx.serialization.core)
    api(projects.components.core.dependangerCore)
}
