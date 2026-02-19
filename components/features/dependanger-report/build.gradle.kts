plugins {
    kotlin("plugin.serialization")
    `maven-publish`
}

description = "Dependanger Report - Dependency report generation"

dependencies {
    api(project(":components:core:dependanger-core"))
    api(project(":components:core:dependanger-effective"))

    // Feature modules for reading extension data
    implementation(project(":components:features:dependanger-updates"))
    implementation(project(":components:features:dependanger-security"))
    implementation(project(":components:features:dependanger-license"))
    implementation(project(":components:features:dependanger-transitive"))

    // Serialization for JSON report output
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // YAML report output
    implementation("com.charleskorn.kaml:kaml:0.67.0")

    // Markdown -> HTML conversion
    implementation("org.commonmark:commonmark:0.24.0")

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
