package io.github.zenhelix.dependanger.integration.serialization

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import io.github.zenhelix.dependanger.core.dsl.dependanger as dslCatalog

class JsonRoundTripE2ETest : IntegrationTestBase() {

    private val format = JsonSerializationFormat()

    @Test
    fun `DSL to JSON round-trip produces identical metadata`() = runTest {
        val dslBlock: DependangerDsl.() -> Unit = {
            versions { version("kotlin", "2.1.20") }
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
            }
        }

        val originalResult = dependanger(ProcessingPreset.DEFAULT, dslBlock = dslBlock).process()
        assertThat(originalResult.isSuccess).isTrue()

        val metadata = dslCatalog(dslBlock).toMetadata()
        val json = format.serialize(metadata)
        val deserialized = format.deserialize(json)

        val roundTripResult = Dependanger.fromMetadata(deserialized).build().process()
        assertThat(roundTripResult.isSuccess).isTrue()

        val originalSuccess = originalResult as DependangerResult.Success
        val roundTripSuccess = roundTripResult as DependangerResult.Success
        assertThat(roundTripSuccess.effective.libraries.keys)
            .isEqualTo(originalSuccess.effective.libraries.keys)
        assertThat(roundTripSuccess.effective.libraries["kotlin-stdlib"]!!.version.valueOrNull)
            .isEqualTo(originalSuccess.effective.libraries["kotlin-stdlib"]!!.version.valueOrNull)
    }

    @Test
    fun `complex catalog survives JSON serialization`() = runTest {
        val metadata = dslCatalog {
            versions {
                version("kotlin", "2.1.20")
                version("ktor", "3.1.1")
                version("assertj", "3.27.3")
            }
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
                library("assertj", "org.assertj:assertj-core", versionRef("assertj"))
                library("old-lib", "com.example:old-lib:1.0.0") {
                    tags("legacy", "deprecated")
                    deprecated(replacedBy = "new-lib", message = "Use new-lib instead")
                }
            }
            plugins {
                plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm", versionRef("kotlin"))
            }
            bundles {
                bundle("ktor") { libraries("ktor-core", "ktor-cio") }
            }
            distributions {
                distribution("server") {
                    spec { byTags { include { anyOf("server") } } }
                }
            }
        }.toMetadata()

        val json = format.serialize(metadata)
        val deserialized = format.deserialize(json)

        assertThat(deserialized.versions).hasSize(3)
        assertThat(deserialized.libraries).hasSize(5)
        assertThat(deserialized.plugins).hasSize(1)
        assertThat(deserialized.bundles).hasSize(1)
        assertThat(deserialized.distributions).hasSize(1)

        val result = Dependanger.fromMetadata(deserialized).build().process()
        assertThat(result.isSuccess).isTrue()
        val success = result as DependangerResult.Success
        assertThat(success.effective.libraries).hasSize(5)
        assertThat(success.effective.plugins).hasSize(1)
        assertThat(success.effective.bundles).hasSize(1)
    }

    @Test
    fun `fromJson produces same effective result as fromDsl`() = runTest {
        val dslBlock: DependangerDsl.() -> Unit = {
            versions {
                version("kotlin", "2.1.20")
                version("ktor", "3.1.1")
            }
            libraries {
                library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
            }
            bundles {
                bundle("ktor") { libraries("ktor-core") }
            }
        }

        val dslResult = Dependanger.fromDsl(dslBlock).build().process()
        assertThat(dslResult.isSuccess).isTrue()

        val metadata = DependangerDsl().apply(dslBlock).toMetadata()
        val json = format.serialize(metadata)
        val jsonResult = Dependanger.fromJson(json).build().process()
        assertThat(jsonResult.isSuccess).isTrue()

        val dslSuccess = dslResult as DependangerResult.Success
        val jsonSuccess = jsonResult as DependangerResult.Success
        assertThat(jsonSuccess.effective.libraries.keys)
            .isEqualTo(dslSuccess.effective.libraries.keys)
        assertThat(jsonSuccess.effective.bundles.keys)
            .isEqualTo(dslSuccess.effective.bundles.keys)
        assertThat(jsonSuccess.effective.versions.keys)
            .isEqualTo(dslSuccess.effective.versions.keys)

        for (alias in dslSuccess.effective.libraries.keys) {
            assertThat(jsonSuccess.effective.libraries[alias]!!.version.valueOrNull)
                .isEqualTo(dslSuccess.effective.libraries[alias]!!.version.valueOrNull)
        }
    }

    @Test
    fun `JSON with all field types deserializes correctly`() = runTest {
        val metadata = dslCatalog {
            versions {
                version("guava", "33.0-jre") {
                    fallback("33.0-android") { jdkBelow(11) }
                }
            }
            libraries {
                library("guava", "com.google.guava:guava", versionRef("guava")) {
                    tags("core", "google")
                }
                library("deprecated-lib", "com.example:deprecated:1.0.0") {
                    deprecated {
                        replacedBy = "new-lib"
                        message = "Migration required"
                        removalVersion = "3.0.0"
                    }
                }
            }
            constraints {
                constraint("com.google.guava:guava", "33.0-jre")
                exclude("org.old:deprecated-lib")
            }
        }.toMetadata()

        val json = format.serialize(metadata)
        val deserialized = format.deserialize(json)

        // Verify versions with fallbacks
        assertThat(deserialized.versions).anyMatch { it.name == "guava" }

        // Verify library tags
        val guavaLib = deserialized.libraries.first { it.alias == "guava" }
        assertThat(guavaLib.tags).containsExactlyInAnyOrder("core", "google")

        // Verify deprecation with all fields
        val deprecatedLib = deserialized.libraries.first { it.alias == "deprecated-lib" }
        assertThat(deprecatedLib.deprecation).isNotNull
        assertThat(deprecatedLib.deprecation!!.replacedBy).isEqualTo("new-lib")
        assertThat(deprecatedLib.deprecation!!.message).isEqualTo("Migration required")
        assertThat(deprecatedLib.deprecation!!.removalVersion).isEqualTo("3.0.0")

        // Verify constraints
        assertThat(deserialized.constraints).hasSize(2)

        // Full round-trip through processing
        val result = Dependanger.fromMetadata(deserialized).preset(ProcessingPreset.DEFAULT).build().process()
        assertThat(result.isSuccess).isTrue()
    }
}
