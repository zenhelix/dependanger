plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

description = "Dependanger API - High-level facade for external consumers"

dependencies {
    api(projects.components.shared.dependangerFeatureModel)
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    implementation(projects.components.core.dependangerMetadataJson)
    api(projects.components.generators.dependangerGeneratorToml)
    api(projects.components.generators.dependangerGeneratorBom)

    implementation(projects.components.features.dependangerMavenResolver)
    implementation(projects.components.features.dependangerUpdates)
    implementation(projects.components.features.dependangerAnalysis)
    implementation(projects.components.features.dependangerReport)
    implementation(projects.components.features.dependangerSecurity)
    implementation(projects.components.features.dependangerLicense)
    implementation(projects.components.features.dependangerTransitive)

    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(projects.components.features.dependangerTransitive)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.slf4j.simple)
}
