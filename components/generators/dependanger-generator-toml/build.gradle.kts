plugins {
    `maven-publish`
}

description = "Dependanger TOML - Version Catalog generator"

dependencies {
    api(project(":components:core:dependanger-core"))
    api(project(":components:core:dependanger-effective"))

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
