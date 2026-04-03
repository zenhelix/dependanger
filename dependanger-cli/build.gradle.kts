plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    `java-test-fixtures`
    application
}

description = "Dependanger CLI - Command-line interface"

dependencies {
    implementation(projects.components.api.dependangerApi)
    // Generators and metadata-json: CLI uses their types (TomlConfig, BomConfig, JsonSerializationFormat)
    implementation(projects.components.generators.dependangerGeneratorToml)
    implementation(projects.components.generators.dependangerGeneratorBom)
    implementation(projects.components.core.dependangerMetadataJson)
    // Feature processors — discovered via ServiceLoader at runtime
    runtimeOnly(projects.components.features.dependangerMavenResolver)
    runtimeOnly(projects.components.features.dependangerUpdates)
    runtimeOnly(projects.components.features.dependangerAnalysis)
    runtimeOnly(projects.components.features.dependangerReport)
    runtimeOnly(projects.components.features.dependangerSecurity)
    runtimeOnly(projects.components.features.dependangerLicense)
    runtimeOnly(projects.components.features.dependangerTransitive)
    implementation(libs.clikt)
    implementation(libs.mordant)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)
    implementation(libs.kotlin.logging.jvm)
    runtimeOnly(libs.logback.classic)

    testFixturesImplementation(projects.components.api.dependangerApi)
    testFixturesImplementation(projects.components.core.dependangerCore)
    testFixturesImplementation(projects.components.core.dependangerEffective)
    testFixturesImplementation(projects.components.core.dependangerMetadataJson)
    testFixturesImplementation(projects.components.features.dependangerUpdates)
    testFixturesImplementation(projects.components.features.dependangerSecurity)
    testFixturesImplementation(projects.components.features.dependangerLicense)
    testFixturesImplementation(projects.components.features.dependangerTransitive)
    testFixturesImplementation(projects.components.features.dependangerAnalysis)
    testFixturesImplementation(projects.components.features.dependangerReport)
    testFixturesImplementation(libs.clikt)
    testFixturesImplementation(libs.mockk)

    testImplementation(testFixtures(project))
}

application {
    mainClass.set("io.github.zenhelix.dependanger.cli.DependangerCliKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.zenhelix.dependanger.cli.DependangerCliKt"
    }
}
