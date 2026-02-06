plugins {
    kotlin("plugin.serialization")
    `maven-publish`
}

description = "Dependanger Core - Models, DSL, and validation"

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")

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
