plugins {
    alias(libs.plugins.dokka) apply false
    `jacoco-report-aggregation`
}

allprojects {
    group = "io.github.zenhelix"
    version = "0.1.0-SNAPSHOT"
}

dependencies {
    subprojects.forEach { subproject ->
        jacocoAggregation(subproject)
    }
}

val leafProjects = subprojects.filter { it.buildFile.exists() }

tasks.register("buildAll") {
    group = "build"
    description = "Build all modules"
    dependsOn(leafProjects.map { it.tasks.named("build") })
}

tasks.register("testAll") {
    group = "verification"
    description = "Run tests in all modules"
    dependsOn(leafProjects.map { it.tasks.named("test") })
}
