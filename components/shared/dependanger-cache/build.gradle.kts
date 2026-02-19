plugins {
    kotlin("plugin.serialization")
    `maven-publish`
}

description = "Dependanger Cache - Shared file-based caching infrastructure"

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
