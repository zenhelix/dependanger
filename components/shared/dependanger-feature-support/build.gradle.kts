plugins {
    id("dependanger.base")
    id("dependanger.publishing")
}

description = "Dependanger Feature Support - Abstract base classes for network-aware processors"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    api(projects.components.shared.dependangerHttpClient)
    implementation(libs.kotlinx.coroutines.core)
}
