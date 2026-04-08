plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Metadata JSON - JSON format serialization for metadata"

dependencies {
    api(projects.components.core.dependangerCore)
    implementation(libs.kotlinx.serialization.json)
}
