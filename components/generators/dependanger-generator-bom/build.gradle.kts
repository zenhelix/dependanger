plugins {
    id("dependanger.base")
    id("dependanger.publishing")
}

description = "Dependanger BOM - Bill of Materials generator"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    api(projects.components.shared.dependangerMavenPom)
    implementation(libs.kotlin.logging.jvm)

    testRuntimeOnly(libs.slf4j.simple)
}
