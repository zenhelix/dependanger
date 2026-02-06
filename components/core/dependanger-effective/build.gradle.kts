plugins {
    kotlin("plugin.serialization")
    `maven-publish`
}

description = "Dependanger Effective - Filtered and enriched metadata model"

dependencies {
    api(project(":components:core:dependanger-core"))

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // Coroutines for parallel processing
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
