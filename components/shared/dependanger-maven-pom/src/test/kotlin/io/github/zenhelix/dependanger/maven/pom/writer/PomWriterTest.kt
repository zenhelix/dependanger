package io.github.zenhelix.dependanger.maven.pom.writer

import io.github.zenhelix.dependanger.maven.pom.model.PomCoordinates
import io.github.zenhelix.dependanger.maven.pom.model.PomDependency
import io.github.zenhelix.dependanger.maven.pom.model.PomDependencyManagement
import io.github.zenhelix.dependanger.maven.pom.model.PomParent
import io.github.zenhelix.dependanger.maven.pom.model.PomProject
import io.github.zenhelix.dependanger.maven.pom.model.PomProperties
import io.github.zenhelix.dependanger.maven.pom.parser.PomParser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PomWriterTest {

    @Nested
    inner class `Minimal POM writing` {

        @Test
        fun `writes minimal POM with coordinates`() {
            val project = minimalProject()
            val writer = PomWriter()

            val result = writer.write(project)

            assertThat(result).contains("<groupId>com.example</groupId>")
            assertThat(result).contains("<artifactId>my-lib</artifactId>")
            assertThat(result).contains("<version>1.0.0</version>")
            assertThat(result).contains("<packaging>jar</packaging>")
        }
    }

    @Nested
    inner class `Parent writing` {

        @Test
        fun `writes POM with parent`() {
            val project = minimalProject().copy(
                parent = PomParent(
                    coordinates = PomCoordinates("com.example", "parent-pom", "2.0.0"),
                    relativePath = "../pom.xml",
                ),
            )
            val writer = PomWriter()

            val result = writer.write(project)

            assertThat(result).contains("<parent>")
            assertThat(result).contains("<groupId>com.example</groupId>")
            assertThat(result).contains("<artifactId>parent-pom</artifactId>")
            assertThat(result).contains("<version>2.0.0</version>")
            assertThat(result).contains("<relativePath>../pom.xml</relativePath>")
            assertThat(result).contains("</parent>")
        }
    }

    @Nested
    inner class `Properties writing` {

        @Test
        fun `writes POM with properties`() {
            val project = minimalProject().copy(
                properties = PomProperties(mapOf("kotlin.version" to "1.9.0")),
            )
            val writer = PomWriter()

            val result = writer.write(project)

            assertThat(result).contains("<properties>")
            assertThat(result).contains("<kotlin.version>1.9.0</kotlin.version>")
            assertThat(result).contains("</properties>")
        }
    }

    @Nested
    inner class `Dependency management writing` {

        @Test
        fun `writes POM with dependencyManagement`() {
            val project = minimalProject().copy(
                dependencyManagement = PomDependencyManagement(
                    listOf(
                        PomDependency(
                            groupId = "org.springframework",
                            artifactId = "spring-framework-bom",
                            version = "6.0.0",
                            type = "pom",
                            scope = "import",
                        ),
                    ),
                ),
            )
            val writer = PomWriter()

            val result = writer.write(project)

            assertThat(result).contains("<dependencyManagement>")
            assertThat(result).contains("<dependencies>")
            assertThat(result).contains("<groupId>org.springframework</groupId>")
            assertThat(result).contains("<type>pom</type>")
            assertThat(result).contains("<scope>import</scope>")
        }

        @Test
        fun `writes POM with dependency comments`() {
            val project = minimalProject().copy(
                dependencyManagement = PomDependencyManagement(
                    listOf(
                        PomDependency("com.example", "dep-a", "1.0.0"),
                        PomDependency("com.example", "dep-b", "2.0.0"),
                    ),
                ),
            )
            val writer = PomWriter()
            val comments = mapOf(0 to "First dependency", 1 to "Second dependency")

            val result = writer.write(project, comments)

            assertThat(result).contains("<!-- First dependency -->")
            assertThat(result).contains("<!-- Second dependency -->")
        }
    }

    @Nested
    inner class `Optional fields` {

        @Test
        fun `optional fields omitted when null`() {
            val project = minimalProject()
            val writer = PomWriter()

            val result = writer.write(project)

            assertThat(result).doesNotContain("<name>")
            assertThat(result).doesNotContain("<description>")
            assertThat(result).doesNotContain("<parent>")
            assertThat(result).doesNotContain("<properties>")
            assertThat(result).doesNotContain("<dependencyManagement>")
        }

        @Test
        fun `optional dependency renders optional=true`() {
            val project = minimalProject().copy(
                dependencyManagement = PomDependencyManagement(
                    listOf(
                        PomDependency("com.example", "dep-a", "1.0.0", optional = true),
                    ),
                ),
            )
            val writer = PomWriter()

            val result = writer.write(project)

            assertThat(result).contains("<optional>true</optional>")
        }
    }

    @Nested
    inner class `PomWriterConfig validation` {

        @Test
        fun `prettyPrint=true with empty indent throws`() {
            assertThatThrownBy { PomWriterConfig(prettyPrint = true, indent = "") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("indent must not be empty")
        }

        @Test
        fun `prettyPrint=false with empty indent is valid`() {
            val config = PomWriterConfig(prettyPrint = false, indent = "")

            assertThat(config.prettyPrint).isFalse()
            assertThat(config.indent).isEmpty()
        }
    }

    @Nested
    inner class `XML declaration` {

        @Test
        fun `XML declaration included when config enables it`() {
            val writer = PomWriter(PomWriterConfig(includeXmlDeclaration = true))

            val result = writer.write(minimalProject())

            assertThat(result).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        }

        @Test
        fun `XML declaration excluded when config disables it`() {
            val writer = PomWriter(PomWriterConfig(includeXmlDeclaration = false))

            val result = writer.write(minimalProject())

            assertThat(result).doesNotContain("<?xml")
        }
    }

    @Nested
    inner class `Round-trip` {

        @Test
        fun `parse then write then parse produces equivalent PomProject`() {
            val original = PomProject(
                coordinates = PomCoordinates("com.example", "round-trip", "1.0.0"),
                packaging = "jar",
                parent = PomParent(
                    coordinates = PomCoordinates("com.example", "parent", "2.0.0"),
                ),
                name = "Round Trip",
                description = "Testing round trip",
                properties = PomProperties(mapOf("key" to "value")),
                dependencyManagement = PomDependencyManagement(
                    listOf(
                        PomDependency("org.example", "dep-a", "3.0.0", scope = "compile"),
                    ),
                ),
            )

            val writer = PomWriter(PomWriterConfig(includeXmlDeclaration = true))
            val xml = writer.write(original)
            val reparsed = PomParser.parse(xml)

            assertThat(reparsed.coordinates).isEqualTo(original.coordinates)
            assertThat(reparsed.packaging).isEqualTo(original.packaging)
            assertThat(reparsed.name).isEqualTo(original.name)
            assertThat(reparsed.description).isEqualTo(original.description)
            assertThat(reparsed.properties.entries).isEqualTo(original.properties.entries)
            assertThat(reparsed.dependencyManagement!!.dependencies)
                .hasSize(original.dependencyManagement!!.dependencies.size)
            assertThat(reparsed.parent!!.coordinates).isEqualTo(original.parent!!.coordinates)
        }
    }

    private companion object {

        private fun minimalProject() = PomProject(
            coordinates = PomCoordinates("com.example", "my-lib", "1.0.0"),
        )
    }
}
