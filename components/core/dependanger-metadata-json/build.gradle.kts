plugins {
    `maven-publish`
}

description = "Dependanger Metadata JSON - JSON format serialization for metadata"

dependencies {
    api(project(":components:core:dependanger-core"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
