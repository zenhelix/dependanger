plugins {
    id("dependanger.base")
    id("dependanger.publishing")
}

description = "Dependanger Analysis - Compatibility and JDK analysis"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    implementation(libs.asm)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.kotlinx.coroutines.test)
}
