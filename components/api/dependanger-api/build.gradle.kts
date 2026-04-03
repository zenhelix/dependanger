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
    // Generators are compileOnly: api provides convenience extensions (toToml, toBom)
    // but does not force generators onto consumers' classpath.
    // Consumers that call these extensions must add generators explicitly.
    compileOnly(projects.components.generators.dependangerGeneratorToml)
    compileOnly(projects.components.generators.dependangerGeneratorBom)

    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    // Generators needed at compile+runtime for tests that call toToml()/toBom()
    testImplementation(projects.components.generators.dependangerGeneratorToml)
    testImplementation(projects.components.generators.dependangerGeneratorBom)
    testRuntimeOnly(libs.slf4j.simple)
    // Feature modules for integration tests — discovered via ServiceLoader at runtime
    testRuntimeOnly(projects.components.features.dependangerMavenResolver)
    testRuntimeOnly(projects.components.features.dependangerUpdates)
    testRuntimeOnly(projects.components.features.dependangerAnalysis)
    testRuntimeOnly(projects.components.features.dependangerReport)
    testRuntimeOnly(projects.components.features.dependangerSecurity)
    testRuntimeOnly(projects.components.features.dependangerLicense)
    testRuntimeOnly(projects.components.features.dependangerTransitive)
}
