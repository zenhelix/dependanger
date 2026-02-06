plugins {
    kotlin("plugin.serialization")
    `maven-publish`
}

description = "Dependanger API - High-level facade for external consumers"

dependencies {
    // Core dependencies
    api(project(":components:core:dependanger-core"))
    api(project(":components:core:dependanger-effective"))
    api(project(":components:core:dependanger-metadata-json"))

    // Generators
    api(project(":components:generators:dependanger-generator-toml"))
    api(project(":components:generators:dependanger-generator-bom"))

    // Features (optional processors) - exposed as api for plugin and CLI consumers
    api(project(":components:features:dependanger-maven-resolver"))
    api(project(":components:features:dependanger-updates"))
    api(project(":components:features:dependanger-analysis"))
    api(project(":components:features:dependanger-report"))
    api(project(":components:features:dependanger-security"))
    api(project(":components:features:dependanger-license"))
    api(project(":components:features:dependanger-transitive"))

    // Serialization - needed transitively by consumers
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // Coroutines - needed transitively by consumers (e.g., runBlocking in plugin tasks)
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // Test dependencies
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
