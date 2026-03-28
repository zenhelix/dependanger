package io.github.zenhelix.dependanger.maven.pom.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PomPropertiesTest {

    @Nested
    inner class `get operator` {

        @Test
        fun `returns value for existing key`() {
            val props = PomProperties(mapOf("key" to "value"))

            assertThat(props["key"]).isEqualTo("value")
        }

        @Test
        fun `returns null for missing key`() {
            val props = PomProperties(mapOf("key" to "value"))

            assertThat(props["missing"]).isNull()
        }
    }

    @Nested
    inner class `plus operator` {

        @Test
        fun `plus PomProperties merges entries`() {
            val a = PomProperties(mapOf("k1" to "v1"))
            val b = PomProperties(mapOf("k2" to "v2"))

            val result = a + b

            assertThat(result["k1"]).isEqualTo("v1")
            assertThat(result["k2"]).isEqualTo("v2")
        }

        @Test
        fun `plus Map merges entries`() {
            val props = PomProperties(mapOf("k1" to "v1"))

            val result = props + mapOf("k2" to "v2")

            assertThat(result["k1"]).isEqualTo("v1")
            assertThat(result["k2"]).isEqualTo("v2")
        }

        @Test
        fun `later values override earlier in plus`() {
            val a = PomProperties(mapOf("key" to "original"))
            val b = PomProperties(mapOf("key" to "overridden"))

            val result = a + b

            assertThat(result["key"]).isEqualTo("overridden")
        }
    }
}
