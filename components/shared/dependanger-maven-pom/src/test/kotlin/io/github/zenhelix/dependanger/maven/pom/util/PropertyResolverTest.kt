package io.github.zenhelix.dependanger.maven.pom.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PropertyResolverTest {

    @Nested
    inner class `resolve` {

        @Test
        fun `no placeholders returns original string`() {
            val resolver = PropertyResolver(mapOf("key" to "value"))

            val result = resolver.resolve("no placeholders here")

            assertThat(result).isEqualTo("no placeholders here")
        }

        @Test
        fun `single property resolved`() {
            val resolver = PropertyResolver(mapOf("version" to "1.0.0"))

            val result = resolver.resolve("\${version}")

            assertThat(result).isEqualTo("1.0.0")
        }

        @Test
        fun `multiple properties resolved`() {
            val resolver = PropertyResolver(
                mapOf("groupId" to "com.example", "version" to "2.0.0"),
            )

            val result = resolver.resolve("\${groupId}:\${version}")

            assertThat(result).isEqualTo("com.example:2.0.0")
        }

        @Test
        fun `unknown property throws PropertyResolutionException`() {
            val resolver = PropertyResolver(emptyMap())

            assertThatThrownBy { resolver.resolve("\${unknown}") }
                .isInstanceOf(PropertyResolutionException::class.java)
                .hasMessageContaining("unknown")
        }

        @Test
        fun `empty property name handled gracefully`() {
            val resolver = PropertyResolver(emptyMap())

            // ${} has empty content between braces - the regex [^}]+ requires at least one char,
            // so it does not match and the string is returned as-is
            val result = resolver.resolve("\${}")

            assertThat(result).isEqualTo("\${}")
        }

        @Test
        fun `property value containing dollar-brace not recursively resolved`() {
            val resolver = PropertyResolver(
                mapOf(
                    "outer" to "\${inner}",
                    "inner" to "deep-value",
                ),
            )

            val result = resolver.resolve("\${outer}")

            assertThat(result).isEqualTo("\${inner}")
        }
    }

    @Nested
    inner class `resolveOrNull` {

        @Test
        fun `returns null for unknown property`() {
            val resolver = PropertyResolver(emptyMap())

            val result = resolver.resolveOrNull("\${missing}")

            assertThat(result).isNull()
        }

        @Test
        fun `returns value for known property`() {
            val resolver = PropertyResolver(mapOf("key" to "found"))

            val result = resolver.resolveOrNull("\${key}")

            assertThat(result).isEqualTo("found")
        }
    }
}
