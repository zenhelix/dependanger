plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

description = "Dependanger CLI - Command-line interface"

dependencies {
    implementation(project(":components:api:dependanger-api"))
    implementation(project(":components:core:dependanger-core"))
    implementation(project(":components:core:dependanger-effective"))
    implementation(project(":components:core:dependanger-metadata-json"))
    implementation(project(":components:generators:dependanger-generator-toml"))
    implementation(project(":components:generators:dependanger-generator-bom"))
    implementation(project(":components:features:dependanger-updates"))
    implementation(project(":components:features:dependanger-analysis"))
    implementation(project(":components:features:dependanger-report"))
    implementation(project(":components:features:dependanger-security"))
    implementation(project(":components:features:dependanger-license"))
    implementation(project(":components:features:dependanger-transitive"))

    // CLI framework
    implementation("com.github.ajalt.clikt:clikt:5.0.3")

    // Terminal formatting
    implementation("com.github.ajalt.mordant:mordant:3.0.2")

    // Coroutines for async processing
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // JSON serialization for output
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

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
