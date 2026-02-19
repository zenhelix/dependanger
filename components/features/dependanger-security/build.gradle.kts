plugins {
    kotlin("plugin.serialization")
    `maven-publish`
}

description = "Dependanger Security - CVE vulnerability scanning via OSV API"

dependencies {
    api(project(":components:core:dependanger-core"))
    api(project(":components:core:dependanger-effective"))
    api(project(":components:shared:dependanger-http-client"))

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    // CVSS
    implementation("us.springett:cvss-calculator:1.5.1")

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
