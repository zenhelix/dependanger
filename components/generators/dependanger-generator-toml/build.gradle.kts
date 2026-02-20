plugins {
    id("dependanger.base")
    id("dependanger.publishing")
}

description = "Dependanger TOML - Version Catalog generator"

dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    implementation(libs.kotlin.logging.jvm)

    testRuntimeOnly(libs.slf4j.simple)
}
