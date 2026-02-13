plugins {
    `maven-publish`
}

description = "Dependanger Maven Resolver - Maven repository integration"

dependencies {
    api(project(":components:core:dependanger-core"))
    api(project(":components:core:dependanger-effective"))
    api(project(":components:shared:dependanger-maven-pom"))
    api(project(":components:shared:dependanger-http-client"))

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

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
