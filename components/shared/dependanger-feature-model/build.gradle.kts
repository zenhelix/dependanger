plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Feature Model - Shared data contracts for inter-processor communication"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    api(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
}
