package io.github.zenhelix.dependanger.generators.bom

import io.github.zenhelix.dependanger.core.model.DeprecationInfo
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class BomGeneratorTest {

    private val defaultConfig = BomConfig(
        groupId = "io.github.zenhelix",
        artifactId = "dependanger-bom",
        version = "1.0.0",
        name = null,
        description = null,
        filename = BomConfig.DEFAULT_FILENAME,
        includeOptionalDependencies = false,
        prettyPrint = true,
        includeDeprecationComments = true,
    )

    private fun emptyMetadata(
        libraries: Map<String, EffectiveLibrary> = emptyMap(),
    ): EffectiveMetadata = EffectiveMetadata(
        schemaVersion = "1.0",
        distribution = null,
        versions = emptyMap(),
        libraries = libraries,
        plugins = emptyMap(),
        bundles = emptyMap(),
        diagnostics = Diagnostics.EMPTY,
        processingInfo = null,
    )

    private fun version(alias: String, value: String): ResolvedVersion =
        ResolvedVersion(alias = alias, value = value, source = VersionSource.DECLARED, originalRef = null)

    private fun library(
        alias: String,
        group: String,
        artifact: String,
        version: ResolvedVersion? = null,
        isDeprecated: Boolean = false,
        deprecation: DeprecationInfo? = null,
        isPlatform: Boolean = false,
    ): EffectiveLibrary = EffectiveLibrary(
        alias = alias,
        group = group,
        artifact = artifact,
        version = version,
        description = null,
        tags = emptySet(),
        requires = null,
        isDeprecated = isDeprecated,
        deprecation = deprecation,
        license = null,
        constraints = emptyList(),
        isPlatform = isPlatform,
    )

    @Nested
    inner class ConfigValidation {
        @Test
        fun `blank groupId throws IllegalArgumentException`() {
            val config = defaultConfig.copy(groupId = "")
            val generator = BomGenerator(config)

            assertThatThrownBy { generator.generate(emptyMetadata()) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("groupId")
        }

        @Test
        fun `blank artifactId throws IllegalArgumentException`() {
            val config = defaultConfig.copy(artifactId = " ")
            val generator = BomGenerator(config)

            assertThatThrownBy { generator.generate(emptyMetadata()) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("artifactId")
        }

        @Test
        fun `blank version throws IllegalArgumentException`() {
            val config = defaultConfig.copy(version = "")
            val generator = BomGenerator(config)

            assertThatThrownBy { generator.generate(emptyMetadata()) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("version")
        }

        @Test
        fun `valid config passes without exception`() {
            val generator = BomGenerator(defaultConfig)
            val result = generator.generate(emptyMetadata())

            assertThat(result).isNotEmpty()
        }
    }

    @Nested
    inner class EmptyMetadata {
        @Test
        fun `empty metadata produces BOM without dependencyManagement`() {
            val generator = BomGenerator(defaultConfig)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<modelVersion>4.0.0</modelVersion>")
            assertThat(result).contains("<groupId>io.github.zenhelix</groupId>")
            assertThat(result).contains("<artifactId>dependanger-bom</artifactId>")
            assertThat(result).contains("<version>1.0.0</version>")
            assertThat(result).contains("<packaging>pom</packaging>")
            assertThat(result).doesNotContain("<dependencyManagement>")
            assertThat(result).doesNotContain("<dependencies>")
        }

        @Test
        fun `empty metadata matches expected format`() {
            val generator = BomGenerator(defaultConfig)
            val result = generator.generate(emptyMetadata())

            assertThat(result).isEqualTo(
                """
                |<?xml version="1.0" encoding="UTF-8"?>
                |<project xmlns="http://maven.apache.org/POM/4.0.0"
                |        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                |        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                |    <modelVersion>4.0.0</modelVersion>
                |    <groupId>io.github.zenhelix</groupId>
                |    <artifactId>dependanger-bom</artifactId>
                |    <version>1.0.0</version>
                |    <packaging>pom</packaging>
                |</project>
                |""".trimMargin()
            )
        }
    }

    @Nested
    inner class SingleLibrary {
        @Test
        fun `single library produces dependency element`() {
            val generator = BomGenerator(defaultConfig)
            val v = version("kotlin", "2.1.20")
            val lib = library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib", version = v)
            val result = generator.generate(emptyMetadata(libraries = mapOf("kotlin-stdlib" to lib)))

            assertThat(result).contains("<dependencyManagement>")
            assertThat(result).contains("<dependencies>")
            assertThat(result).contains("<groupId>org.jetbrains.kotlin</groupId>")
            assertThat(result).contains("<artifactId>kotlin-stdlib</artifactId>")
            assertThat(result).contains("<version>2.1.20</version>")
        }
    }

    @Nested
    inner class SortingOrder {
        @Test
        fun `libraries sorted by groupId then artifactId`() {
            val generator = BomGenerator(defaultConfig)
            val libraries = mapOf(
                "spring-web" to library("spring-web", "org.springframework", "spring-web", version = version("spring", "6.0.0")),
                "kotlin-stdlib" to library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib", version = version("kotlin", "2.1.20")),
                "kotlin-reflect" to library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect", version = version("kotlin", "2.1.20")),
                "assertj" to library("assertj", "org.assertj", "assertj-core", version = version("assertj", "3.27.3")),
            )
            val result = generator.generate(emptyMetadata(libraries = libraries))

            val artifactIds = Regex("<artifactId>(.*?)</artifactId>")
                .findAll(result)
                .map { it.groupValues[1] }
                .filter { it != "dependanger-bom" }
                .toList()

            assertThat(artifactIds).containsExactly(
                "assertj-core",
                "kotlin-reflect",
                "kotlin-stdlib",
                "spring-web",
            )
        }
    }

    @Nested
    inner class LibraryWithoutVersion {
        @Test
        fun `library without version is excluded`() {
            val generator = BomGenerator(defaultConfig)
            val lib = library("managed-dep", "com.example", "managed")
            val result = generator.generate(emptyMetadata(libraries = mapOf("managed-dep" to lib)))

            assertThat(result).doesNotContain("<dependencyManagement>")
            assertThat(result).doesNotContain("com.example")
        }

        @Test
        fun `all libraries without version produces BOM without dependencyManagement`() {
            val generator = BomGenerator(defaultConfig)
            val libraries = mapOf(
                "lib1" to library("lib1", "com.example", "lib1"),
                "lib2" to library("lib2", "com.example", "lib2"),
            )
            val result = generator.generate(emptyMetadata(libraries = libraries))

            assertThat(result).doesNotContain("<dependencyManagement>")
        }

        @Test
        fun `library without version is excluded while others are included`() {
            val generator = BomGenerator(defaultConfig)
            val libraries = mapOf(
                "with-version" to library("with-version", "com.example", "with-version", version = version("v", "1.0.0")),
                "no-version" to library("no-version", "com.example", "no-version"),
            )
            val result = generator.generate(emptyMetadata(libraries = libraries))

            assertThat(result).contains("<artifactId>with-version</artifactId>")
            assertThat(result).doesNotContain("no-version")
        }
    }

    @Nested
    inner class DeprecationComments {
        @Test
        fun `deprecated library with full info`() {
            val generator = BomGenerator(defaultConfig)
            val v = version("spring-boot", "3.4.0")
            val lib = library(
                alias = "spring-boot-starter",
                group = "org.springframework.boot",
                artifact = "spring-boot-starter",
                version = v,
                isDeprecated = true,
                deprecation = DeprecationInfo(
                    message = "Legacy starter",
                    replacedBy = "spring-boot-starter-web",
                    since = "3.0",
                    removalVersion = "4.0",
                ),
            )
            val result = generator.generate(emptyMetadata(libraries = mapOf("spring-boot-starter" to lib)))

            assertThat(result).contains("<!-- DEPRECATED: Legacy starter. Use spring-boot-starter-web instead. Removal: 4.0 -->")
        }

        @Test
        fun `deprecated library with no deprecation object`() {
            val generator = BomGenerator(defaultConfig)
            val lib = library(
                alias = "old-lib",
                group = "com.example",
                artifact = "old-lib",
                version = version("v", "1.0.0"),
                isDeprecated = true,
                deprecation = null,
            )
            val result = generator.generate(emptyMetadata(libraries = mapOf("old-lib" to lib)))

            assertThat(result).contains("<!-- DEPRECATED -->")
        }

        @Test
        fun `deprecated library with only message`() {
            val generator = BomGenerator(defaultConfig)
            val lib = library(
                alias = "old-lib",
                group = "com.example",
                artifact = "old-lib",
                version = version("v", "1.0.0"),
                isDeprecated = true,
                deprecation = DeprecationInfo(message = "No longer maintained", replacedBy = null, since = null, removalVersion = null),
            )
            val result = generator.generate(emptyMetadata(libraries = mapOf("old-lib" to lib)))

            assertThat(result).contains("<!-- DEPRECATED: No longer maintained -->")
        }

        @Test
        fun `deprecated library with only replacedBy`() {
            val generator = BomGenerator(defaultConfig)
            val lib = library(
                alias = "old-lib",
                group = "com.example",
                artifact = "old-lib",
                version = version("v", "1.0.0"),
                isDeprecated = true,
                deprecation = DeprecationInfo(message = null, replacedBy = "new-lib", since = null, removalVersion = null),
            )
            val result = generator.generate(emptyMetadata(libraries = mapOf("old-lib" to lib)))

            assertThat(result).contains("<!-- DEPRECATED. Use new-lib instead -->")
        }

        @Test
        fun `deprecation comments disabled`() {
            val config = defaultConfig.copy(includeDeprecationComments = false)
            val generator = BomGenerator(config)
            val lib = library(
                alias = "old-lib",
                group = "com.example",
                artifact = "old-lib",
                version = version("v", "1.0.0"),
                isDeprecated = true,
                deprecation = DeprecationInfo(message = "Deprecated", replacedBy = "new-lib", since = null, removalVersion = "2.0"),
            )
            val result = generator.generate(emptyMetadata(libraries = mapOf("old-lib" to lib)))

            assertThat(result).doesNotContain("<!-- DEPRECATED")
            assertThat(result).contains("<artifactId>old-lib</artifactId>")
        }

        @Test
        fun `double dash in deprecation comment replaced with single dashes`() {
            val generator = BomGenerator(defaultConfig)
            val lib = library(
                alias = "old-lib",
                group = "com.example",
                artifact = "old-lib",
                version = version("v", "1.0.0"),
                isDeprecated = true,
                deprecation = DeprecationInfo(message = "Use new--improved lib", replacedBy = null, since = null, removalVersion = null),
            )
            val result = generator.generate(emptyMetadata(libraries = mapOf("old-lib" to lib)))

            assertThat(result).contains("<!-- DEPRECATED: Use new- -improved lib -->")
            // Extract comment content (between <!-- and -->) and verify no "--" inside
            val commentContent = Regex("<!--(.+?)-->").find(result)!!.groupValues[1]
            assertThat(commentContent).doesNotContain("--")
                .describedAs("XML comment body must not contain --")
        }
    }

    @Nested
    inner class PlatformLibrary {
        @Test
        fun `platform library has type pom and scope import`() {
            val generator = BomGenerator(defaultConfig)
            val v = version("spring-boot", "3.4.0")
            val lib = library(
                alias = "spring-boot-bom",
                group = "org.springframework.boot",
                artifact = "spring-boot-dependencies",
                version = v,
                isPlatform = true,
            )
            val result = generator.generate(emptyMetadata(libraries = mapOf("spring-boot-bom" to lib)))

            assertThat(result).contains("<type>pom</type>")
            assertThat(result).contains("<scope>import</scope>")
        }

        @Test
        fun `non-platform library does not have type or scope`() {
            val generator = BomGenerator(defaultConfig)
            val v = version("kotlin", "2.1.20")
            val lib = library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib", version = v)
            val result = generator.generate(emptyMetadata(libraries = mapOf("kotlin-stdlib" to lib)))

            assertThat(result).doesNotContain("<type>")
            assertThat(result).doesNotContain("<scope>")
        }
    }

    @Nested
    inner class OptionalDependencies {
        @Test
        fun `optional dependencies included when enabled`() {
            val config = defaultConfig.copy(includeOptionalDependencies = true)
            val generator = BomGenerator(config)
            val v = version("kotlin", "2.1.20")
            val lib = library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib", version = v)
            val result = generator.generate(emptyMetadata(libraries = mapOf("kotlin-stdlib" to lib)))

            assertThat(result).contains("<optional>true</optional>")
        }

        @Test
        fun `optional dependencies not included when disabled`() {
            val generator = BomGenerator(defaultConfig)
            val v = version("kotlin", "2.1.20")
            val lib = library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib", version = v)
            val result = generator.generate(emptyMetadata(libraries = mapOf("kotlin-stdlib" to lib)))

            assertThat(result).doesNotContain("<optional>")
        }

        @Test
        fun `platform library with optional has all elements`() {
            val config = defaultConfig.copy(includeOptionalDependencies = true)
            val generator = BomGenerator(config)
            val v = version("spring-boot", "3.4.0")
            val lib = library(
                alias = "spring-boot-bom",
                group = "org.springframework.boot",
                artifact = "spring-boot-dependencies",
                version = v,
                isPlatform = true,
            )
            val result = generator.generate(emptyMetadata(libraries = mapOf("spring-boot-bom" to lib)))

            assertThat(result).contains("<type>pom</type>")
            assertThat(result).contains("<scope>import</scope>")
            assertThat(result).contains("<optional>true</optional>")
        }
    }

    @Nested
    inner class PrettyPrint {
        @Test
        fun `pretty print enabled uses indentation`() {
            val generator = BomGenerator(defaultConfig)
            val v = version("kotlin", "2.1.20")
            val lib = library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib", version = v)
            val result = generator.generate(emptyMetadata(libraries = mapOf("kotlin-stdlib" to lib)))

            assertThat(result).contains("    <modelVersion>")
            assertThat(result).contains("        <dependencies>")
            assertThat(result).contains("            <dependency>")
            assertThat(result).contains("                <groupId>")
        }

        @Test
        fun `pretty print disabled produces minified XML`() {
            val config = defaultConfig.copy(prettyPrint = false)
            val generator = BomGenerator(config)
            val v = version("kotlin", "2.1.20")
            val lib = library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib", version = v)
            val result = generator.generate(emptyMetadata(libraries = mapOf("kotlin-stdlib" to lib)))

            assertThat(result).doesNotContain("\n")
            assertThat(result).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            assertThat(result).contains("<project xmlns=")
            assertThat(result).contains("<dependency><groupId>org.jetbrains.kotlin</groupId><artifactId>kotlin-stdlib</artifactId><version>2.1.20</version></dependency>")
        }
    }

    @Nested
    inner class NameAndDescription {
        @Test
        fun `name included when provided`() {
            val config = defaultConfig.copy(name = "Dependanger BOM")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<name>Dependanger BOM</name>")
        }

        @Test
        fun `description included when provided`() {
            val config = defaultConfig.copy(description = "Generated by Dependanger")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<description>Generated by Dependanger</description>")
        }

        @Test
        fun `name and description omitted when null`() {
            val generator = BomGenerator(defaultConfig)
            val result = generator.generate(emptyMetadata())

            assertThat(result).doesNotContain("<name>")
            assertThat(result).doesNotContain("<description>")
        }

        @Test
        fun `empty name not included`() {
            val config = defaultConfig.copy(name = "")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).doesNotContain("<name>")
        }

        @Test
        fun `blank name not included`() {
            val config = defaultConfig.copy(name = "   ")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).doesNotContain("<name>")
        }

        @Test
        fun `empty description not included`() {
            val config = defaultConfig.copy(description = "")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).doesNotContain("<description>")
        }
    }

    @Nested
    inner class XmlStructure {
        @Test
        fun `packaging is always pom`() {
            val generator = BomGenerator(defaultConfig)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<packaging>pom</packaging>")
        }

        @Test
        fun `model version is 4_0_0`() {
            val generator = BomGenerator(defaultConfig)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<modelVersion>4.0.0</modelVersion>")
        }

        @Test
        fun `Maven namespace attributes present`() {
            val generator = BomGenerator(defaultConfig)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("xmlns=\"http://maven.apache.org/POM/4.0.0\"")
            assertThat(result).contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
            assertThat(result).contains("xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"")
        }

        @Test
        fun `BOM coordinates match config`() {
            val config = defaultConfig.copy(groupId = "com.acme", artifactId = "my-bom", version = "2.5.0")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<groupId>com.acme</groupId>")
            assertThat(result).contains("<artifactId>my-bom</artifactId>")
            assertThat(result).contains("<version>2.5.0</version>")
        }

        @Test
        fun `SNAPSHOT version in BOM coordinates`() {
            val config = defaultConfig.copy(version = "1.0.0-SNAPSHOT")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<version>1.0.0-SNAPSHOT</version>")
        }

        @Test
        fun `SNAPSHOT version in dependency`() {
            val generator = BomGenerator(defaultConfig)
            val v = version("lib", "2.0.0-SNAPSHOT")
            val lib = library("snapshot-lib", "com.example", "snapshot-lib", version = v)
            val result = generator.generate(emptyMetadata(libraries = mapOf("snapshot-lib" to lib)))

            assertThat(result).contains("<version>2.0.0-SNAPSHOT</version>")
        }
    }

    @Nested
    inner class SpecialCharacters {
        @Test
        fun `ampersand in values is escaped`() {
            val config = defaultConfig.copy(groupId = "com.a&b")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<groupId>com.a&amp;b</groupId>")
        }

        @Test
        fun `angle brackets in values are escaped`() {
            val config = defaultConfig.copy(name = "BOM <test>")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<name>BOM &lt;test&gt;</name>")
        }

        @Test
        fun `quotes in values are escaped`() {
            val config = defaultConfig.copy(description = "A \"test\" BOM")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<description>A &quot;test&quot; BOM</description>")
        }

        @Test
        fun `apostrophe in values is escaped`() {
            val config = defaultConfig.copy(name = "John's BOM")
            val generator = BomGenerator(config)
            val result = generator.generate(emptyMetadata())

            assertThat(result).contains("<name>John&apos;s BOM</name>")
        }
    }

    @Nested
    inner class WriteMethod {
        @Test
        fun `writes file to specified path`(@TempDir tempDir: Path) {
            val generator = BomGenerator(defaultConfig)
            val content = "<?xml version=\"1.0\"?><project/>"
            generator.write(content, tempDir)

            val file = tempDir.resolve(BomConfig.DEFAULT_FILENAME)
            assertThat(file).exists()
            assertThat(file.readText()).isEqualTo(content)
        }

        @Test
        fun `creates parent directories`(@TempDir tempDir: Path) {
            val generator = BomGenerator(defaultConfig)
            val content = "<project/>"
            val nested = tempDir.resolve("a/b/c")
            generator.write(content, nested)

            val file = nested.resolve(BomConfig.DEFAULT_FILENAME)
            assertThat(file).exists()
            assertThat(file.readText()).isEqualTo(content)
        }

        @Test
        fun `writes with UTF-8 encoding`(@TempDir tempDir: Path) {
            val generator = BomGenerator(defaultConfig)
            val content = "<?xml version=\"1.0\"?><!-- Кириллица и спецсимволы: \u00e9\u00e0\u00fc --><project/>"
            generator.write(content, tempDir)

            val file = tempDir.resolve(BomConfig.DEFAULT_FILENAME)
            assertThat(file.readText(Charsets.UTF_8)).isEqualTo(content)
        }

        @Test
        fun `overwrites existing file`(@TempDir tempDir: Path) {
            val generator = BomGenerator(defaultConfig)
            val file = tempDir.resolve(BomConfig.DEFAULT_FILENAME)
            java.nio.file.Files.writeString(file, "old content")

            generator.write("new content", tempDir)
            assertThat(file.readText()).isEqualTo("new content")
        }

        @Test
        fun `uses config filename`(@TempDir tempDir: Path) {
            val config = defaultConfig.copy(filename = "custom.pom.xml")
            val generator = BomGenerator(config)
            generator.write("content", tempDir)

            assertThat(tempDir.resolve("custom.pom.xml")).exists()
            assertThat(tempDir.resolve(BomConfig.DEFAULT_FILENAME)).doesNotExist()
        }
    }

    @Nested
    inner class FullEndToEnd {
        @Test
        fun `full metadata end-to-end`() {
            val config = defaultConfig.copy(
                name = "Dependanger BOM",
                description = "Generated by Dependanger",
            )
            val generator = BomGenerator(config)

            val kotlinVersion = version("kotlin", "2.1.20")
            val springBootVersion = version("spring-boot", "3.4.0")

            val metadata = emptyMetadata(
                libraries = mapOf(
                    "kotlin-stdlib" to library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib", version = kotlinVersion),
                    "kotlin-reflect" to library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect", version = kotlinVersion),
                    "spring-boot-starter" to library(
                        alias = "spring-boot-starter",
                        group = "org.springframework.boot",
                        artifact = "spring-boot-starter",
                        version = springBootVersion,
                        isDeprecated = true,
                        deprecation = DeprecationInfo(
                            message = "Legacy starter",
                            replacedBy = "spring-boot-starter-web",
                            since = "3.0",
                            removalVersion = "4.0",
                        ),
                    ),
                    "spring-boot-starter-web" to library(
                        "spring-boot-starter-web",
                        "org.springframework.boot",
                        "spring-boot-starter-web",
                        version = springBootVersion,
                    ),
                ),
            )

            val result = generator.generate(metadata)

            assertThat(result).isEqualTo(
                """
                |<?xml version="1.0" encoding="UTF-8"?>
                |<project xmlns="http://maven.apache.org/POM/4.0.0"
                |        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                |        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                |    <modelVersion>4.0.0</modelVersion>
                |    <groupId>io.github.zenhelix</groupId>
                |    <artifactId>dependanger-bom</artifactId>
                |    <version>1.0.0</version>
                |    <packaging>pom</packaging>
                |    <name>Dependanger BOM</name>
                |    <description>Generated by Dependanger</description>
                |
                |    <dependencyManagement>
                |        <dependencies>
                |            <dependency>
                |                <groupId>org.jetbrains.kotlin</groupId>
                |                <artifactId>kotlin-reflect</artifactId>
                |                <version>2.1.20</version>
                |            </dependency>
                |            <dependency>
                |                <groupId>org.jetbrains.kotlin</groupId>
                |                <artifactId>kotlin-stdlib</artifactId>
                |                <version>2.1.20</version>
                |            </dependency>
                |            <!-- DEPRECATED: Legacy starter. Use spring-boot-starter-web instead. Removal: 4.0 -->
                |            <dependency>
                |                <groupId>org.springframework.boot</groupId>
                |                <artifactId>spring-boot-starter</artifactId>
                |                <version>3.4.0</version>
                |            </dependency>
                |            <dependency>
                |                <groupId>org.springframework.boot</groupId>
                |                <artifactId>spring-boot-starter-web</artifactId>
                |                <version>3.4.0</version>
                |            </dependency>
                |        </dependencies>
                |    </dependencyManagement>
                |</project>
                |""".trimMargin()
            )
        }

        @Test
        fun `generate and write round trip`(@TempDir tempDir: Path) {
            val generator = BomGenerator(defaultConfig)
            val v = version("kotlin", "2.1.20")
            val lib = library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib", version = v)
            val metadata = emptyMetadata(libraries = mapOf("kotlin-stdlib" to lib))

            val xml = generator.generate(metadata)
            generator.write(xml, tempDir)

            val file = tempDir.resolve(BomConfig.DEFAULT_FILENAME)
            assertThat(file).exists()
            assertThat(file.readText()).isEqualTo(xml)
        }
    }
}
