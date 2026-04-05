plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger Analysis - Compatibility and JDK analysis"

dependencies {
    implementation(projects.components.core.dependangerCore)
    implementation(projects.components.core.dependangerEffective)
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(libs.asm)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.slf4j.simple)
}
