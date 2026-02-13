plugins {
    `maven-publish`
}

description = "Dependanger HTTP Client - Shared HTTP client infrastructure with retry and error handling"

dependencies {
    // HTTP client (exposed as API for consumers)
    api("io.ktor:ktor-client-core:3.1.1")
    api("io.ktor:ktor-client-cio:3.1.1")
    api("io.ktor:ktor-client-content-negotiation:3.1.1")
    api("io.ktor:ktor-serialization-kotlinx-json:3.1.1")

    // Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // Test
    testImplementation("io.ktor:ktor-client-mock:3.1.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
