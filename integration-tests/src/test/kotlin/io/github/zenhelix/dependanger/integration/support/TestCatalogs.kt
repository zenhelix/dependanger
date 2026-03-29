package io.github.zenhelix.dependanger.integration.support

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.VersionConstraintType

/**
 * Pre-built DSL catalogs for different integration test scenarios.
 */
object TestCatalogs {

    /**
     * Minimal catalog: 2 versions, 2 libraries, 1 plugin, 1 bundle.
     */
    fun minimal(): DependangerDsl.() -> Unit = {
        versions {
            version("kotlin", "2.1.20")
            version("ktor", "3.1.1")
        }
        libraries {
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
        }
        plugins {
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin"))
        }
        bundles {
            bundle("kotlin-essentials") {
                libraries("kotlin-stdlib")
            }
        }
    }

    /**
     * Standard catalog: 5 versions, 8 libraries, 3 plugins, 2 bundles, 1 distribution.
     */
    fun standard(): DependangerDsl.() -> Unit = {
        versions {
            version("kotlin", "2.1.20")
            version("ktor", "3.1.1")
            version("coroutines", "1.10.1")
            version("serialization", "1.8.0")
            version("logback", "1.5.15")
        }
        libraries {
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
            library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
            library("ktor-json", "io.ktor:ktor-serialization-kotlinx-json", versionRef("ktor"))
            library("coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core", versionRef("coroutines"))
            library("serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json", versionRef("serialization"))
            library("logback", "ch.qos.logback:logback-classic", versionRef("logback"))
            library("assertj", "org.assertj:assertj-core:3.27.3")
        }
        plugins {
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin"))
            plugin("kotlin-serialization", "org.jetbrains.kotlin.plugin.serialization", versionRef("kotlin"))
            plugin("ktor", "io.ktor.plugin", versionRef("ktor"))
        }
        bundles {
            bundle("kotlin-essentials") {
                libraries("kotlin-stdlib", "coroutines-core", "serialization-json")
            }
            bundle("ktor-client") {
                libraries("ktor-core", "ktor-cio", "ktor-json")
            }
        }
        distributions {
            distribution("server") {
                spec {
                    byBundles {
                        include("kotlin-essentials", "ktor-client")
                    }
                }
            }
        }
    }

    /**
     * Complex catalog: 10+ versions, 20+ libraries (including deprecated, platform/BOM imports),
     * 5+ plugins, 4 bundles (with extends), 3 distributions, version fallbacks,
     * compatibility rules, settings with updateCheck/security/license enabled.
     */
    fun complex(): DependangerDsl.() -> Unit = {
        versions {
            version("kotlin", "2.1.20")
            version("ktor", "3.1.1")
            version("coroutines", "1.10.1")
            version("serialization", "1.8.0")
            version("logback", "1.5.15")
            version("spring-boot", "3.4.3")
            version("jackson", "2.18.3")
            version("slf4j", "2.0.16")
            version("mockk", "1.13.16")
            version("junit", "5.11.4")
            version("assertj", "3.27.3")
        }
        libraries {
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            library("coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core", versionRef("coroutines"))
            library("serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json", versionRef("serialization"))
            library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
            library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
            library("ktor-json", "io.ktor:ktor-serialization-kotlinx-json", versionRef("ktor"))
            library("ktor-server-core", "io.ktor:ktor-server-core", versionRef("ktor"))
            library("ktor-server-netty", "io.ktor:ktor-server-netty", versionRef("ktor"))
            library("spring-boot-starter", "org.springframework.boot:spring-boot-starter", versionRef("spring-boot"))
            library("spring-boot-starter-web", "org.springframework.boot:spring-boot-starter-web", versionRef("spring-boot"))
            library("jackson-kotlin", "com.fasterxml.jackson.module:jackson-module-kotlin", versionRef("jackson"))
            library("slf4j-api", "org.slf4j:slf4j-api", versionRef("slf4j"))
            library("logback", "ch.qos.logback:logback-classic", versionRef("logback"))
            library("mockk", "io.mockk:mockk", versionRef("mockk"))
            library("junit-api", "org.junit.jupiter:junit-jupiter-api", versionRef("junit"))
            library("junit-engine", "org.junit.jupiter:junit-jupiter-engine", versionRef("junit"))
            library("assertj", "org.assertj:assertj-core", versionRef("assertj"))
            library("old-http-client", "org.apache.httpcomponents:httpclient:4.5.14") {
                deprecated(replacedBy = "ktor-core", message = "Migrate to Ktor client")
            }
            library("vulnerable-lib", "com.example:vulnerable-lib:1.0.0")
            library("safe-lib", "com.safe:safe-lib:2.0.0")
            platformLibrary("spring-boot-bom", "org.springframework.boot:spring-boot-dependencies", versionRef("spring-boot"))
        }
        plugins {
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin"))
            plugin("kotlin-serialization", "org.jetbrains.kotlin.plugin.serialization", versionRef("kotlin"))
            plugin("ktor", "io.ktor.plugin", versionRef("ktor"))
            plugin("spring-boot", "org.springframework.boot", versionRef("spring-boot"))
            plugin("spring-dependency-management", "io.spring.dependency-management:1.1.7")
        }
        bundles {
            bundle("kotlin-essentials") {
                libraries("kotlin-stdlib", "coroutines-core", "serialization-json")
            }
            bundle("ktor-client") {
                libraries("ktor-core", "ktor-cio", "ktor-json")
            }
            bundle("ktor-server") {
                libraries("ktor-server-core", "ktor-server-netty")
            }
            bundle("testing") {
                libraries("mockk", "junit-api", "junit-engine", "assertj")
                extends("kotlin-essentials")
            }
        }
        distributions {
            distribution("android") {
                spec {
                    byBundles {
                        include("kotlin-essentials", "ktor-client")
                    }
                    byGroups {
                        exclude {
                            matching("org.springframework.*")
                        }
                    }
                }
            }
            distribution("server") {
                spec {
                    byBundles {
                        include("kotlin-essentials", "ktor-server")
                    }
                    byGroups {
                        include {
                            matching("org.springframework.*", "io.ktor*", "org.jetbrains.*", "ch.qos.logback*")
                        }
                    }
                }
            }
            distribution("minimal") {
                spec {
                    byBundles {
                        include("kotlin-essentials")
                    }
                }
            }
        }
        compatibility {
            jdkRequirement("spring-boot-jdk17") {
                matches = "org.springframework.boot:*"
                minJdk = 17
                severity = Severity.ERROR
                message = "Spring Boot 3.x requires JDK 17+"
            }
            versionConstraint("ktor-same-version") {
                libraries("ktor-core", "ktor-cio", "ktor-json", "ktor-server-core", "ktor-server-netty")
                constraint = VersionConstraintType.SAME_VERSION
                severity = Severity.ERROR
                message = "All Ktor modules must use the same version"
            }
            mutualExclusion("ktor-vs-spring") {
                libraries("ktor-server-core", "spring-boot-starter-web")
                severity = Severity.WARNING
                message = "Using both Ktor server and Spring Boot web may cause conflicts"
            }
        }
        settings {
            updateCheck {
                enabled = true
                includePrerelease = false
            }
            securityCheck {
                enabled = true
                failOnVulnerability = Severity.ERROR
            }
            licenseCheck {
                enabled = true
                allowedLicenses = listOf("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause")
                failOnDenied = true
            }
        }
    }

    /**
     * Catalog with JDK and Kotlin version fallbacks.
     */
    fun withFallbacks(): DependangerDsl.() -> Unit = {
        versions {
            version("kotlin", "2.1.20") {
                fallback("2.0.21") { kotlinVersionBelow("2.1.0") }
            }
            version("ktor", "3.1.1") {
                fallback("2.3.12") { jdkBelow(11) }
            }
            version("coroutines", "1.10.1") {
                fallback("1.8.1") { jdkBelow(11) }
                fallback("1.9.0") { jdkBetween(11, 16) }
            }
        }
        libraries {
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
            library("coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core", versionRef("coroutines"))
        }
    }

    /**
     * Catalog with deprecated libraries.
     */
    fun withDeprecated(): DependangerDsl.() -> Unit = {
        versions {
            version("kotlin", "2.1.20")
            version("ktor", "3.1.1")
        }
        libraries {
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
            library("old-http-client", "org.apache.httpcomponents:httpclient:4.5.14") {
                deprecated(replacedBy = "ktor-core", message = "Migrate to Ktor client")
            }
            library("old-json-lib", "com.google.code.gson:gson:2.10.1") {
                deprecated {
                    replacedBy = "kotlinx-serialization"
                    message = "Use kotlinx.serialization instead"
                    since = "2024-01-01"
                    removalVersion = "3.0.0"
                }
            }
        }
    }

    /**
     * Catalog with global constraints.
     */
    fun withConstraints(): DependangerDsl.() -> Unit = {
        versions {
            version("kotlin", "2.1.20")
            version("ktor", "3.1.1")
            version("logback", "1.5.15")
        }
        libraries {
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
            library("logback", "ch.qos.logback:logback-classic", versionRef("logback"))
        }
        constraints {
            constraint("org.jetbrains.kotlin:*", "2.1.20")
            exclude("com.example:banned-lib")
            substitute("old.group:old-artifact", "new.group:new-artifact") {
                because = "Library was relocated"
            }
        }
    }
}
