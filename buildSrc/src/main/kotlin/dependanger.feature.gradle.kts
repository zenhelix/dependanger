plugins {
    id("dependanger.base")
    id("dependanger.serialization")
    id("dependanger.publishing")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(project(":components:core:dependanger-core"))
    "implementation"(project(":components:core:dependanger-effective"))
    "implementation"(libs.findLibrary("kotlinx-serialization-json").get())
    "implementation"(libs.findLibrary("kotlin-logging-jvm").get())

    "testImplementation"(libs.findLibrary("ktor-client-mock").get())
    "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
}
