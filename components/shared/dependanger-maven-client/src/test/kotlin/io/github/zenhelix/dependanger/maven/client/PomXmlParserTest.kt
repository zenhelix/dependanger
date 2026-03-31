package io.github.zenhelix.dependanger.maven.client

import io.github.zenhelix.dependanger.maven.pom.parser.PomParseException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PomXmlParserTest {

    private val parser = PomXmlParser()

    @Nested
    inner class `valid BOM POM XML` {

        @Test
        fun `is parsed to PomParseResult with dependencies`() {
            val pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>example-bom</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.apache</groupId>
                                <artifactId>commons-lang</artifactId>
                                <version>3.12.0</version>
                            </dependency>
                            <dependency>
                                <groupId>com.google</groupId>
                                <artifactId>guava</artifactId>
                                <version>31.1-jre</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()

            val result = parser.parseBomContent(pomXml)

            assertThat(result.dependencies).hasSize(2)
            assertThat(result.dependencies[0]).isEqualTo(
                RawBomDependency(
                    group = "org.apache",
                    artifact = "commons-lang",
                    version = "3.12.0",
                    scope = null,
                    type = null,
                ),
            )
            assertThat(result.dependencies[1]).isEqualTo(
                RawBomDependency(
                    group = "com.google",
                    artifact = "guava",
                    version = "31.1-jre",
                    scope = null,
                    type = null,
                ),
            )
        }

        @Test
        fun `extracts scope and type from dependencies`() {
            val pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>example-bom</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework</groupId>
                                <artifactId>spring-framework-bom</artifactId>
                                <version>6.0.0</version>
                                <scope>import</scope>
                                <type>pom</type>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()

            val result = parser.parseBomContent(pomXml)

            assertThat(result.dependencies).hasSize(1)
            assertThat(result.dependencies.first().scope).isEqualTo("import")
            assertThat(result.dependencies.first().type).isEqualTo("pom")
        }

        @Test
        fun `parses BOM with no dependencies to empty list`() {
            val pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>empty-bom</artifactId>
                    <version>1.0.0</version>
                </project>
            """.trimIndent()

            val result = parser.parseBomContent(pomXml)

            assertThat(result.dependencies).isEmpty()
            assertThat(result.parent).isNull()
        }
    }

    @Nested
    inner class `property resolution in dependencies` {

        @Test
        fun `properties are resolved in dependency coordinates`() {
            val pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>props-bom</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <spring.version>6.1.0</spring.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework</groupId>
                                <artifactId>spring-core</artifactId>
                                <version>${'$'}{spring.version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()

            val result = parser.parseBomContent(pomXml)

            assertThat(result.properties).containsEntry("spring.version", "6.1.0")
            assertThat(result.dependencies.first().version).isEqualTo("\${spring.version}")

            val resolvedVersion = parser.resolveProperty(
                result.dependencies.first().version,
                result.properties,
            )
            assertThat(resolvedVersion).isEqualTo("6.1.0")
        }

        @Test
        fun `synthetic project properties are available for resolution`() {
            val pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>my-bom</artifactId>
                    <version>2.0.0</version>
                </project>
            """.trimIndent()

            val result = parser.parseBomContent(pomXml)

            assertThat(result.properties)
                .containsEntry("project.groupId", "com.example")
                .containsEntry("project.artifactId", "my-bom")
                .containsEntry("project.version", "2.0.0")
        }

        @Test
        fun `resolveProperty resolves nested property reference`() {
            val properties = mapOf("my.version" to "3.0.0")

            val resolved = parser.resolveProperty("\${my.version}", properties)

            assertThat(resolved).isEqualTo("3.0.0")
        }

        @Test
        fun `resolveProperty returns literal value when no placeholder present`() {
            val resolved = parser.resolveProperty("1.2.3", emptyMap())

            assertThat(resolved).isEqualTo("1.2.3")
        }
    }

    @Nested
    inner class `parent POM` {

        @Test
        fun `parent properties are extracted from POM`() {
            val pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <parent>
                        <groupId>com.example.parent</groupId>
                        <artifactId>parent-pom</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>child-bom</artifactId>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.test</groupId>
                                <artifactId>test-lib</artifactId>
                                <version>1.0.0</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()

            val result = parser.parseBomContent(pomXml)

            assertThat(result.parent).isNotNull
            assertThat(result.parent!!.group).isEqualTo("com.example.parent")
            assertThat(result.parent!!.artifact).isEqualTo("parent-pom")
            assertThat(result.parent!!.version).isEqualTo("1.0.0")
        }

        @Test
        fun `parent synthetic properties are available`() {
            val pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <parent>
                        <groupId>com.example.parent</groupId>
                        <artifactId>parent-pom</artifactId>
                        <version>5.0.0</version>
                    </parent>
                    <artifactId>child-bom</artifactId>
                </project>
            """.trimIndent()

            val result = parser.parseBomContent(pomXml)

            assertThat(result.properties)
                .containsEntry("project.parent.groupId", "com.example.parent")
                .containsEntry("project.parent.artifactId", "parent-pom")
                .containsEntry("project.parent.version", "5.0.0")
        }
    }

    @Nested
    inner class `missing version reference` {

        @Test
        fun `unresolved property in resolveProperty throws IllegalStateException`() {
            assertThatThrownBy {
                parser.resolveProperty("\${unknown.version}", emptyMap())
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Unresolved property")
        }

        @Test
        fun `dependency with empty version is parsed as empty string`() {
            val pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>missing-ver-bom</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.test</groupId>
                                <artifactId>no-version</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()

            val result = parser.parseBomContent(pomXml)

            assertThat(result.dependencies).hasSize(1)
            assertThat(result.dependencies.first().version).isEmpty()
        }
    }

    @Nested
    inner class `invalid XML` {

        @Test
        fun `produces parse error for malformed XML`() {
            val invalidXml = "<project><unclosed"

            assertThatThrownBy {
                parser.parseBomContent(invalidXml)
            }.isInstanceOf(PomParseException::class.java)
        }

        @Test
        fun `produces parse error for empty string`() {
            assertThatThrownBy {
                parser.parseBomContent("")
            }.isInstanceOf(Exception::class.java)
        }

        @Test
        fun `produces parse error when required artifactId is missing`() {
            val pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>com.example</groupId>
                    <version>1.0.0</version>
                </project>
            """.trimIndent()

            assertThatThrownBy {
                parser.parseBomContent(pomXml)
            }.isInstanceOf(PomParseException::class.java)
                .hasMessageContaining("artifactId")
        }
    }

    @Nested
    inner class `circular property references` {

        @Test
        fun `self-referencing property throws on resolution`() {
            val properties = mapOf("a" to "\${b}", "b" to "\${a}")

            val resolved = parser.resolveProperty("\${a}", properties)
            assertThat(resolved).isEqualTo("\${b}")
        }

        @Test
        fun `unresolved property after substitution throws on second resolve attempt`() {
            val properties = mapOf("a" to "\${nonexistent}")

            val firstPass = parser.resolveProperty("\${a}", properties)
            assertThat(firstPass).isEqualTo("\${nonexistent}")

            assertThatThrownBy {
                parser.resolveProperty(firstPass, properties)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Unresolved property")
        }
    }
}
