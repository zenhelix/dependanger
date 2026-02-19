plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

description = "Dependanger CLI - Command-line interface"

dependencies {
    implementation(project(":components:api:dependanger-api"))

    // CLI framework
    implementation("com.github.ajalt.clikt:clikt:5.0.3")

    // Terminal formatting
    implementation("com.github.ajalt.mordant:mordant:3.0.2")

    // Coroutines for async processing
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // JSON serialization for output
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // YAML serialization for config loading
    implementation("com.charleskorn.kaml:kaml:0.67.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.4")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.16")
}

application {
    mainClass.set("io.github.zenhelix.dependanger.cli.DependangerCliKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.zenhelix.dependanger.cli.DependangerCliKt"
    }
}
