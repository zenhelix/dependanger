plugins {
    id("dependanger.feature")
}

description = "Dependanger Analysis - Compatibility and JDK analysis"

dependencies {
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(libs.asm)
    implementation(libs.kotlinx.coroutines.core)
}
