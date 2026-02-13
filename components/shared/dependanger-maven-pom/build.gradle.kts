plugins {
    kotlin("plugin.serialization")
    `maven-publish`
}

description = "Dependanger Maven POM - Shared Maven POM parsing and writing"

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")

    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
