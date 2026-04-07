enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "dependanger"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

include(":dependanger-gradle-plugin")
include(":dependanger-cli")
include(":integration-tests")

// Core modules (MODEL layer)
include(":components:core:dependanger-core")
include(":components:core:dependanger-metadata-json")
include(":components:core:dependanger-effective")

// Shared modules
include(":components:shared:dependanger-feature-model")
include(":components:shared:dependanger-maven-pom")
include(":components:shared:dependanger-http-client")
include(":components:shared:dependanger-cache")
include(":components:shared:dependanger-feature-support")

// Shared integration client modules
include(":components:shared:integrations:dependanger-maven-http-client")
include(":components:shared:integrations:dependanger-osv-http-client")
include(":components:shared:integrations:dependanger-clearlydefined-http-client")

// API module (API layer)
include(":components:api:dependanger-api")

// Generator modules (SPI implementations)
include(":components:generators:dependanger-generator-toml")
include(":components:generators:dependanger-generator-bom")

// Feature modules (FEATURES layer - SPI processors)
include(":components:features:dependanger-maven-resolver")
include(":components:features:dependanger-updates")
include(":components:features:dependanger-analysis")
include(":components:features:dependanger-report")
include(":components:features:dependanger-security")
include(":components:features:dependanger-license")
include(":components:features:dependanger-transitive")
