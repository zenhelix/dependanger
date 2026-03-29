plugins {
    id("dependanger.base")
    id("dependanger.serialization")
}

description = "Dependanger Integration Tests"

kotlin {
    // Integration tests are not a published module — no explicitApi needed
    explicitApi = null
}

dependencies {
    testImplementation(projects.components.api.dependangerApi)
    testImplementation(projects.dependangerCli)
    testImplementation(testFixtures(projects.dependangerCli))
    testImplementation(projects.components.core.dependangerCore)
    testImplementation(projects.components.core.dependangerEffective)
    testImplementation(projects.components.core.dependangerMetadataJson)
    testImplementation(projects.components.shared.dependangerFeatureModel)
    testImplementation(projects.components.shared.dependangerHttpClient)
    testImplementation(projects.components.shared.dependangerMavenPom)
    testImplementation(projects.components.shared.dependangerCache)

    testImplementation(projects.components.generators.dependangerGeneratorToml)
    testImplementation(projects.components.generators.dependangerGeneratorBom)

    testImplementation(projects.components.features.dependangerUpdates)
    testImplementation(projects.components.features.dependangerSecurity)
    testImplementation(projects.components.features.dependangerLicense)
    testImplementation(projects.components.features.dependangerTransitive)
    testImplementation(projects.components.features.dependangerAnalysis)
    testImplementation(projects.components.features.dependangerReport)
    testImplementation(projects.components.features.dependangerMavenResolver)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.clikt)

    testRuntimeOnly(libs.slf4j.simple)
}

tasks.test {
    // Integration tests may need more memory and time
    maxHeapSize = "512m"
    systemProperty("kotlinx.coroutines.test.default_timeout", "60s")
}
