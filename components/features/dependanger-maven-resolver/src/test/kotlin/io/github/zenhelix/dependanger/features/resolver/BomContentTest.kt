package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BomContentTest {

    @Nested
    inner class `EMPTY constant` {

        @Test
        fun `has no dependencies`() {
            assertThat(BomContent.EMPTY.dependencies).isEmpty()
        }

        @Test
        fun `has no properties`() {
            assertThat(BomContent.EMPTY.properties).isEmpty()
        }
    }

    @Nested
    inner class `serialization round-trip` {

        private val json = Json {
            prettyPrint = false
            encodeDefaults = true
        }

        @Test
        fun `BomContent survives JSON serialization and deserialization`() {
            val original = BomContent(
                dependencies = listOf(
                    BomDependency(coordinate = MavenCoordinate("com.example", "lib-a"), version = "1.0.0"),
                    BomDependency(coordinate = MavenCoordinate("org.test", "lib-b"), version = "2.3.4"),
                ),
                properties = mapOf("spring.version" to "6.1.0", "jackson.version" to "2.15.0"),
            )

            val serialized = json.encodeToString(BomContent.serializer(), original)
            val deserialized = json.decodeFromString(BomContent.serializer(), serialized)

            assertThat(deserialized).isEqualTo(original)
        }

        @Test
        fun `empty BomContent survives serialization round-trip`() {
            val serialized = json.encodeToString(BomContent.serializer(), BomContent.EMPTY)
            val deserialized = json.decodeFromString(BomContent.serializer(), serialized)

            assertThat(deserialized).isEqualTo(BomContent.EMPTY)
        }

        @Test
        fun `BomDependency survives JSON serialization and deserialization`() {
            val original = BomDependency(
                coordinate = MavenCoordinate("io.ktor", "ktor-client-core"),
                version = "2.3.7",
            )

            val serialized = json.encodeToString(BomDependency.serializer(), original)
            val deserialized = json.decodeFromString(BomDependency.serializer(), serialized)

            assertThat(deserialized).isEqualTo(original)
        }
    }

    @Nested
    inner class `data class equality` {

        @Test
        fun `two BomContent instances with same data are equal`() {
            val deps = listOf(BomDependency(coordinate = MavenCoordinate("g", "a"), version = "1.0"))
            val props = mapOf("key" to "value")

            val first = BomContent(dependencies = deps, properties = props)
            val second = BomContent(dependencies = deps, properties = props)

            assertThat(first).isEqualTo(second)
            assertThat(first.hashCode()).isEqualTo(second.hashCode())
        }

        @Test
        fun `BomContent with different dependencies are not equal`() {
            val first = BomContent(
                dependencies = listOf(BomDependency(coordinate = MavenCoordinate("g", "a"), version = "1.0")),
                properties = emptyMap(),
            )
            val second = BomContent(
                dependencies = listOf(BomDependency(coordinate = MavenCoordinate("g", "b"), version = "1.0")),
                properties = emptyMap(),
            )

            assertThat(first).isNotEqualTo(second)
        }
    }
}
