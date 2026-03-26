package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.maven.pom.model.PomCoordinates
import io.github.zenhelix.dependanger.maven.pom.model.PomDependency
import io.github.zenhelix.dependanger.maven.pom.model.PomDependencyManagement
import io.github.zenhelix.dependanger.maven.pom.model.PomProject
import io.github.zenhelix.dependanger.maven.pom.model.PomProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PomDependencyExtractionTest {

    private val defaultScopes = listOf("compile")

    @Nested
    inner class `dependencies are extracted with resolved properties` {

        @Test
        fun `simple dependency is extracted as-is`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(groupId = "org.lib", artifactId = "commons", version = "1.0"),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].group).isEqualTo("org.lib")
            assertThat(result[0].artifact).isEqualTo("commons")
            assertThat(result[0].version).isEqualTo("1.0")
        }

        @Test
        fun `property placeholder in version is resolved`() {
            val pom = pomProject(
                properties = PomProperties(entries = mapOf("commons.version" to "3.5")),
                dependencies = listOf(
                    PomDependency(
                        groupId = "org.lib",
                        artifactId = "commons",
                        version = "\${commons.version}",
                    ),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("3.5")
        }

        @Test
        fun `property placeholder in groupId is resolved`() {
            val pom = pomProject(
                properties = PomProperties(entries = mapOf("lib.group" to "org.resolved")),
                dependencies = listOf(
                    PomDependency(
                        groupId = "\${lib.group}",
                        artifactId = "commons",
                        version = "1.0",
                    ),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].group).isEqualTo("org.resolved")
        }
    }

    @Nested
    inner class `scope filtering` {

        @Test
        fun `only compile scope dependencies are returned by default`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(groupId = "a", artifactId = "compile-dep", version = "1.0", scope = "compile"),
                    PomDependency(groupId = "a", artifactId = "test-dep", version = "1.0", scope = "test"),
                    PomDependency(groupId = "a", artifactId = "runtime-dep", version = "1.0", scope = "runtime"),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].artifact).isEqualTo("compile-dep")
        }

        @Test
        fun `null scope defaults to compile`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(groupId = "a", artifactId = "no-scope", version = "1.0"),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].scope).isEqualTo("compile")
        }

        @Test
        fun `multiple scopes can be requested`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(groupId = "a", artifactId = "compile-dep", version = "1.0", scope = "compile"),
                    PomDependency(groupId = "a", artifactId = "runtime-dep", version = "1.0", scope = "runtime"),
                    PomDependency(groupId = "a", artifactId = "test-dep", version = "1.0", scope = "test"),
                )
            )

            val result = PomDependencyExtractor.extract(pom, listOf("compile", "runtime"), includeOptional = false)

            assertThat(result).hasSize(2)
            assertThat(result.map { it.artifact })
                .containsExactlyInAnyOrder("compile-dep", "runtime-dep")
        }
    }

    @Nested
    inner class `optional dependency handling` {

        @Test
        fun `optional dependencies are excluded by default`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(groupId = "a", artifactId = "required", version = "1.0"),
                    PomDependency(groupId = "a", artifactId = "optional-lib", version = "1.0", optional = true),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].artifact).isEqualTo("required")
        }

        @Test
        fun `optional dependencies are included when configured`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(groupId = "a", artifactId = "required", version = "1.0"),
                    PomDependency(groupId = "a", artifactId = "optional-lib", version = "1.0", optional = true),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = true)

            assertThat(result).hasSize(2)
            val optionalDep = result.first { it.artifact == "optional-lib" }
            assertThat(optionalDep.optional).isTrue()
        }
    }

    @Nested
    inner class `missing version resolved from dependencyManagement` {

        @Test
        fun `version from dependencyManagement is used when dependency has no version`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(groupId = "org.lib", artifactId = "managed"),
                ),
                dependencyManagement = PomDependencyManagement(
                    dependencies = listOf(
                        PomDependency(groupId = "org.lib", artifactId = "managed", version = "4.0"),
                    )
                ),
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("4.0")
        }

        @Test
        fun `dependency with explicit version ignores dependencyManagement`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(groupId = "org.lib", artifactId = "managed", version = "1.0"),
                ),
                dependencyManagement = PomDependencyManagement(
                    dependencies = listOf(
                        PomDependency(groupId = "org.lib", artifactId = "managed", version = "4.0"),
                    )
                ),
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("1.0")
        }

        @Test
        fun `version is null when neither dependency nor management has it`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(groupId = "org.lib", artifactId = "unmanaged"),
                ),
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isNull()
        }
    }

    @Nested
    inner class `property placeholders resolved correctly` {

        @Test
        fun `project version placeholder is resolved`() {
            val pom = pomProject(
                coordinates = PomCoordinates("com.example", "app", "5.0"),
                dependencies = listOf(
                    PomDependency(
                        groupId = "com.example",
                        artifactId = "sibling",
                        version = "\${project.version}",
                    ),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("5.0")
        }

        @Test
        fun `parent version placeholder is resolved`() {
            val pom = pomProject(
                parentCoordinates = PomCoordinates("com.parent", "parent-pom", "7.0"),
                dependencies = listOf(
                    PomDependency(
                        groupId = "com.example",
                        artifactId = "child",
                        version = "\${project.parent.version}",
                    ),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("7.0")
        }

        @Test
        fun `dependency management version properties are resolved`() {
            val pom = pomProject(
                properties = PomProperties(entries = mapOf("managed.version" to "8.0")),
                dependencies = listOf(
                    PomDependency(groupId = "org.lib", artifactId = "managed"),
                ),
                dependencyManagement = PomDependencyManagement(
                    dependencies = listOf(
                        PomDependency(
                            groupId = "org.lib",
                            artifactId = "managed",
                            version = "\${managed.version}",
                        ),
                    )
                ),
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("8.0")
        }

        @Test
        fun `unresolvable property placeholder results in null dependency`() {
            val pom = pomProject(
                dependencies = listOf(
                    PomDependency(
                        groupId = "\${unknown.group}",
                        artifactId = "lib",
                        version = "1.0",
                    ),
                )
            )

            val result = PomDependencyExtractor.extract(pom, defaultScopes, includeOptional = false)

            assertThat(result).isEmpty()
        }
    }

    private fun pomProject(
        coordinates: PomCoordinates = PomCoordinates("com.example", "test-project", "1.0"),
        parentCoordinates: PomCoordinates? = null,
        properties: PomProperties = PomProperties(),
        dependencies: List<PomDependency> = emptyList(),
        dependencyManagement: PomDependencyManagement? = null,
    ): PomProject = PomProject(
        coordinates = coordinates,
        parent = parentCoordinates?.let { io.github.zenhelix.dependanger.maven.pom.model.PomParent(coordinates = it) },
        properties = properties,
        dependencies = dependencies,
        dependencyManagement = dependencyManagement,
    )
}
