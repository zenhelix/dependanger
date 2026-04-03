package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.dependanger
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MetadataSerializationBehaviorTest {

    private val format = JsonSerializationFormat()

    @Nested
    inner class `JSON round-trip preserves data` {

        @Test
        fun `simple library round-trips through JSON`() = runTest {
            val metadata = dependanger {
                versions { version("ktor", "3.1.1") }
                libraries { library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) }
            }.toMetadata()

            val json = format.serialize(metadata)
            val deserialized = format.deserialize(json)

            val result = Dependanger.fromMetadata(deserialized).build().process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("ktor-core")
            assertThat((result as DependangerResult.Success).effective.libraries["ktor-core"]!!.version.valueOrNull).isEqualTo("3.1.1")
        }

        @Test
        fun `complex metadata round-trips`() {
            val metadata = dependanger {
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
                    bundle("ktor") { libraries("ktor-core") }
                }
                distributions {
                    distribution("server") {
                        spec { byTags { include { anyOf("server") } } }
                    }
                }
            }.toMetadata()

            val json = format.serialize(metadata)
            val deserialized = format.deserialize(json)

            assertThat(deserialized.versions).hasSize(2)
            assertThat(deserialized.libraries).hasSize(2)
            assertThat(deserialized.plugins).hasSize(1)
            assertThat(deserialized.bundles).hasSize(1)
            assertThat(deserialized.distributions).hasSize(1)
        }

        @Test
        fun `library with tags and deprecation round-trips`() {
            val metadata = dependanger {
                libraries {
                    library("old-lib", "com.example:old-lib:1.0.0") {
                        tags("legacy", "deprecated")
                        deprecated(replacedBy = "new-lib", message = "Use new-lib instead")
                    }
                }
            }.toMetadata()

            val json = format.serialize(metadata)
            val deserialized = format.deserialize(json)

            val lib = deserialized.libraries.first { it.alias == "old-lib" }
            assertThat(lib.tags).containsExactlyInAnyOrder("legacy", "deprecated")
            assertThat(lib.deprecation).isNotNull
            assertThat(lib.deprecation!!.replacedBy).isEqualTo("new-lib")
            assertThat(lib.deprecation!!.message).isEqualTo("Use new-lib instead")
        }

        @Test
        fun `constraints round-trip`() {
            val metadata = dependanger {
                constraints {
                    constraint("com.google.guava:guava", "33.0-jre")
                    exclude("org.old:deprecated-lib")
                    substitute("org.old:old-module", "org.new:new-module")
                }
            }.toMetadata()

            val json = format.serialize(metadata)
            val deserialized = format.deserialize(json)

            assertThat(deserialized.constraints).hasSize(3)
        }
    }

    @Nested
    inner class `File-based serialization` {

        @Test
        fun `write and read from file preserves metadata`(@TempDir tempDir: Path) = runTest {
            val metadata = dependanger {
                versions { version("kotlin", "2.1.20") }
                libraries {
                    library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                }
            }.toMetadata()

            val filePath = tempDir.resolve("metadata.json")
            format.write(metadata, filePath)
            val readBack = format.read(filePath)

            val result = Dependanger.fromMetadata(readBack).build().process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("kotlin-stdlib")
            assertThat((result as DependangerResult.Success).effective.libraries["kotlin-stdlib"]!!.version.valueOrNull).isEqualTo("2.1.20")
        }
    }

    @Nested
    inner class `fromJson builder integration` {

        @Test
        fun `fromJson processes metadata correctly`() = runTest {
            val metadata = dependanger {
                versions { version("assertj", "3.27.3") }
                libraries {
                    library("assertj-core", "org.assertj:assertj-core", versionRef("assertj"))
                }
            }.toMetadata()

            val json = format.serialize(metadata)
            val result = Dependanger.fromJson(json).build().process()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("assertj-core")
            assertThat((result as DependangerResult.Success).effective.libraries["assertj-core"]!!.version.valueOrNull).isEqualTo("3.27.3")
        }

        @Test
        fun `fromJson with distribution filters correctly`() = runTest {
            val metadata = dependanger {
                libraries {
                    library("android-lib", "com.android:lib:1.0") { tags("android") }
                    library("server-lib", "com.server:lib:2.0") { tags("server") }
                }
                distributions {
                    distribution("android") {
                        spec { byTags { include { anyOf("android") } } }
                    }
                }
            }.toMetadata()

            val json = format.serialize(metadata)
            val result = Dependanger.fromJson(json).build().process(distribution = "android")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries).containsKey("android-lib")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("server-lib")
        }
    }

    @Nested
    inner class `Schema version` {

        @Test
        fun `metadata contains schema version`() {
            val metadata = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.toMetadata()

            val json = format.serialize(metadata)

            assertThat(json).contains("\"schemaVersion\"")
            assertThat(json).containsPattern("\"schemaVersion\"\\s*:\\s*\"1\\.0\"")
        }

        @Test
        fun `deserialized metadata preserves schema version`() {
            val metadata = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.toMetadata()

            val json = format.serialize(metadata)
            val deserialized = format.deserialize(json)

            assertThat(deserialized.schemaVersion).isEqualTo("1.0")
        }
    }

    @Nested
    inner class `Error handling` {

        @Test
        fun `invalid JSON throws exception`() {
            assertThatThrownBy { Dependanger.fromJson("invalid") }
                .isInstanceOf(DependangerConfigurationException::class.java)
                .hasMessageContaining("Failed to parse metadata from JSON")
        }

        @Test
        fun `empty JSON object deserializes gracefully`() {
            assertThatThrownBy { Dependanger.fromJson("{}") }
                .isInstanceOf(DependangerConfigurationException::class.java)
        }
    }

    @Nested
    inner class `Processing DSL block is not serialized` {

        @Test
        fun `processing section does not appear in JSON`() {
            val metadata = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
                processing {
                    preset = ProcessingPreset.STRICT
                    disableProcessor("some-processor")
                }
            }.toMetadata()

            val json = format.serialize(metadata)

            assertThat(json).doesNotContain("\"processing\"")
            assertThat(json).doesNotContain("\"disabledProcessors\"")
            assertThat(json).doesNotContain("some-processor")
            assertThat(json).doesNotContain("STRICT")
        }
    }
}
