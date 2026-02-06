plugins {
    kotlin("plugin.serialization")
    `maven-publish`
}

description = "Dependanger Report - Dependency report generation"

dependencies {
    api(project(":components:core:dependanger-core"))
    api(project(":components:core:dependanger-effective"))

    // Serialization for JSON report output
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

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
