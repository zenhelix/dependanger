package io.github.zenhelix.dependanger.maven.pom.parser

import io.github.zenhelix.dependanger.maven.pom.model.PomCoordinates
import io.github.zenhelix.dependanger.maven.pom.model.PomDependency
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PomParserTest {

    @Nested
    inner class `Minimal POM parsing` {

        @Test
        fun `parses minimal POM with groupId, artifactId and version`() {
            val result = PomParser.parse(minimalPom())

            assertThat(result.coordinates.groupId).isEqualTo("com.example")
            assertThat(result.coordinates.artifactId).isEqualTo("my-lib")
            assertThat(result.coordinates.version).isEqualTo("1.0.0")
            assertThat(result.modelVersion).isEqualTo("4.0.0")
            assertThat(result.packaging).isEqualTo("jar")
        }

        @Test
        fun `parses POM with packaging element`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.packaging).isEqualTo("pom")
        }

        @Test
        fun `parses POM with name and description`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <name>My Library</name>
                    <description>A test library</description>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.name).isEqualTo("My Library")
            assertThat(result.description).isEqualTo("A test library")
        }

        @Test
        fun `empty version tag treated as null and inherits from parent`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <artifactId>my-lib</artifactId>
                    <version></version>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-pom</artifactId>
                        <version>2.0.0</version>
                    </parent>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.coordinates.version).isEqualTo("2.0.0")
        }
    }

    @Nested
    inner class `Parent element parsing` {

        @Test
        fun `parses POM with parent element and inherits groupId`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <artifactId>my-lib</artifactId>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-pom</artifactId>
                        <version>2.0.0</version>
                    </parent>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.coordinates.groupId).isEqualTo("com.example")
            assertThat(result.coordinates.version).isEqualTo("2.0.0")
            assertThat(result.parent).isNotNull
            assertThat(result.parent!!.coordinates).isEqualTo(
                PomCoordinates("com.example", "parent-pom", "2.0.0")
            )
        }

        @Test
        fun `parses POM with parent element and inherits version`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.child</groupId>
                    <artifactId>child-lib</artifactId>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-pom</artifactId>
                        <version>3.0.0</version>
                    </parent>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.coordinates.groupId).isEqualTo("com.example.child")
            assertThat(result.coordinates.version).isEqualTo("3.0.0")
        }
    }

    @Nested
    inner class `Missing required elements` {

        @Test
        fun `missing artifactId throws PomParseException`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <version>1.0.0</version>
                </project>
            """.trimIndent()

            assertThatThrownBy { PomParser.parse(pom) }
                .isInstanceOf(PomParseException::class.java)
                .hasMessageContaining("artifactId")
        }

        @Test
        fun `missing groupId without parent throws PomParseException`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                </project>
            """.trimIndent()

            assertThatThrownBy { PomParser.parse(pom) }
                .isInstanceOf(PomParseException::class.java)
                .hasMessageContaining("groupId")
        }

        @Test
        fun `missing version without parent throws PomParseException`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                </project>
            """.trimIndent()

            assertThatThrownBy { PomParser.parse(pom) }
                .isInstanceOf(PomParseException::class.java)
                .hasMessageContaining("version")
        }
    }

    @Nested
    inner class `Properties parsing` {

        @Test
        fun `parses POM with properties`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <kotlin.version>1.9.0</kotlin.version>
                        <java.version>17</java.version>
                    </properties>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.properties["kotlin.version"]).isEqualTo("1.9.0")
            assertThat(result.properties["java.version"]).isEqualTo("17")
        }
    }

    @Nested
    inner class `Dependencies parsing` {

        @Test
        fun `parses POM with dependencies`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>1.9.0</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.dependencies).hasSize(1)
            assertThat(result.dependencies[0].groupId).isEqualTo("org.jetbrains.kotlin")
            assertThat(result.dependencies[0].artifactId).isEqualTo("kotlin-stdlib")
            assertThat(result.dependencies[0].version).isEqualTo("1.9.0")
        }

        @Test
        fun `optional dependency parsed correctly`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.slf4j</groupId>
                            <artifactId>slf4j-api</artifactId>
                            <version>2.0.0</version>
                            <optional>true</optional>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.dependencies[0].optional).isTrue()
        }

        @Test
        fun `dependency missing groupId throws PomParseException`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>1.9.0</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()

            assertThatThrownBy { PomParser.parse(pom) }
                .isInstanceOf(PomParseException::class.java)
                .hasMessageContaining("groupId")
        }

        @Test
        fun `dependency missing artifactId throws PomParseException`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <version>1.9.0</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()

            assertThatThrownBy { PomParser.parse(pom) }
                .isInstanceOf(PomParseException::class.java)
                .hasMessageContaining("artifactId")
        }
    }

    @Nested
    inner class `Dependency management parsing` {

        @Test
        fun `parses POM with dependencyManagement`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework</groupId>
                                <artifactId>spring-framework-bom</artifactId>
                                <version>6.0.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.dependencyManagement).isNotNull
            assertThat(result.dependencyManagement!!.dependencies).hasSize(1)
            assertThat(result.dependencyManagement!!.dependencies[0].groupId)
                .isEqualTo("org.springframework")
            assertThat(result.dependencyManagement!!.dependencies[0].type).isEqualTo("pom")
            assertThat(result.dependencyManagement!!.dependencies[0].scope).isEqualTo("import")
        }

        @Test
        fun `dependencies inside dependencyManagement not counted as top-level`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework</groupId>
                                <artifactId>spring-core</artifactId>
                                <version>6.0.0</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.dependencies).isEmpty()
            assertThat(result.dependencyManagement).isNotNull
            assertThat(result.dependencyManagement!!.dependencies).hasSize(1)
        }
    }

    @Nested
    inner class `Licenses parsing` {

        @Test
        fun `parses POM with licenses`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                    <licenses>
                        <license>
                            <name>Apache License, Version 2.0</name>
                            <url>https://www.apache.org/licenses/LICENSE-2.0</url>
                        </license>
                    </licenses>
                </project>
            """.trimIndent()

            val result = PomParser.parse(pom)

            assertThat(result.licenses).hasSize(1)
            assertThat(result.licenses[0].name).isEqualTo("Apache License, Version 2.0")
            assertThat(result.licenses[0].url).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0")
        }
    }

    @Nested
    inner class `Security` {

        @Test
        fun `XXE protection - DOCTYPE throws PomParseException`() {
            val pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>1.0.0</version>
                </project>
            """.trimIndent()

            assertThatThrownBy { PomParser.parse(pom) }
                .isInstanceOf(PomParseException::class.java)
        }
    }

    @Nested
    inner class `PomDependency behavior` {

        @Test
        fun `isPlatformImport returns true for scope=import type=pom`() {
            val dep = PomDependency(
                groupId = "org.springframework",
                artifactId = "spring-framework-bom",
                version = "6.0.0",
                scope = "import",
                type = "pom",
            )

            assertThat(dep.isPlatformImport()).isTrue()
        }

        @Test
        fun `isPlatformImport returns false for regular dependency`() {
            val dep = PomDependency(
                groupId = "org.springframework",
                artifactId = "spring-core",
                version = "6.0.0",
            )

            assertThat(dep.isPlatformImport()).isFalse()
        }
    }

    private companion object {

        private fun minimalPom(
            groupId: String = "com.example",
            artifactId: String = "my-lib",
            version: String = "1.0.0",
        ) = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>$groupId</groupId>
                <artifactId>$artifactId</artifactId>
                <version>$version</version>
            </project>
        """.trimIndent()
    }
}
