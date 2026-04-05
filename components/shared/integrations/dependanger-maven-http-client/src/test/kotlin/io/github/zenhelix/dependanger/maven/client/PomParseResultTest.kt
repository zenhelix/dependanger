package io.github.zenhelix.dependanger.maven.client

import io.github.zenhelix.dependanger.maven.client.model.ParentPom
import io.github.zenhelix.dependanger.maven.client.model.PomParseResult
import io.github.zenhelix.dependanger.maven.client.model.RawBomDependency
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PomParseResultTest {

    @Nested
    inner class `PomParseResult data class` {

        @Test
        fun `equality based on content`() {
            val deps = listOf(RawBomDependency("g", "a", "1.0", null, null))
            val parent = ParentPom("pg", "pa", "1.0")
            val props = mapOf("key" to "value")

            val first = PomParseResult(props, parent, deps)
            val second = PomParseResult(props, parent, deps)

            assertThat(first).isEqualTo(second)
            assertThat(first.hashCode()).isEqualTo(second.hashCode())
        }

        @Test
        fun `null parent is valid`() {
            val result = PomParseResult(emptyMap(), null, emptyList())
            assertThat(result.parent).isNull()
        }
    }

    @Nested
    inner class `ParentPom data class` {

        @Test
        fun `carries group, artifact, version`() {
            val parent = ParentPom("com.example", "parent-pom", "2.0.0")
            assertThat(parent.group).isEqualTo("com.example")
            assertThat(parent.artifact).isEqualTo("parent-pom")
            assertThat(parent.version).isEqualTo("2.0.0")
        }
    }

    @Nested
    inner class `RawBomDependency data class` {

        @Test
        fun `nullable scope and type`() {
            val dep = RawBomDependency("g", "a", "1.0", null, null)
            assertThat(dep.scope).isNull()
            assertThat(dep.type).isNull()
        }

        @Test
        fun `with scope and type`() {
            val dep = RawBomDependency("g", "a", "1.0", "import", "pom")
            assertThat(dep.scope).isEqualTo("import")
            assertThat(dep.type).isEqualTo("pom")
        }
    }
}
