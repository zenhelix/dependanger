package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DistributionFilteringBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) { preset(preset) }

    @Nested
    inner class `Group pattern filtering` {

        @Test
        fun `include by exact group matches libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0") { tags("spring") }
                    library("spring-context", "org.springframework:spring-context:6.1.0") { tags("spring") }
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1") { tags("ktor") }
                }
                distributions {
                    distribution("spring-only") {
                        spec { byGroups { include { matching("org.springframework") } } }
                    }
                }
            }.process(distribution = "spring-only")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("spring-core", "spring-context")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("ktor-core")
        }

        @Test
        fun `include by glob pattern matches libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                    library("spring-boot", "org.springframework.boot:spring-boot:3.3.0")
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                }
                distributions {
                    distribution("all-spring") {
                        spec { byGroups { include { matching("org.springframework*") } } }
                    }
                }
            }.process(distribution = "all-spring")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("spring-core", "spring-boot")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("ktor-core")
        }

        @Test
        fun `exclude by group removes matching libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("modern-lib", "com.modern:lib:2.0.0")
                    library("legacy-lib", "com.legacy:old-lib:0.9.0")
                    library("legacy-util", "com.legacy.util:helper:0.5.0")
                }
                distributions {
                    distribution("no-legacy") {
                        spec { byGroups { exclude { matching("com.legacy*") } } }
                    }
                }
            }.process(distribution = "no-legacy")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys).containsExactly("modern-lib")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("legacy-lib")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("legacy-util")
        }

        @Test
        fun `include and exclude combined in group filter`() = runTest {
            val result = dependanger {
                libraries {
                    library("spring-core", "org.springframework:spring-core:6.1.0")
                    library("spring-internal", "org.springframework.internal:debug:1.0.0")
                    library("spring-boot", "org.springframework.boot:spring-boot:3.3.0")
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                }
                distributions {
                    distribution("spring-public") {
                        spec {
                            byGroups {
                                include { matching("org.springframework*") }
                                exclude { matching("org.springframework.internal*") }
                            }
                        }
                    }
                }
            }.process(distribution = "spring-public")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("spring-core", "spring-boot")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("spring-internal")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("ktor-core")
        }
    }

    @Nested
    inner class `Bundle-based filtering` {

        @Test
        fun `include by bundle includes all bundle member libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                    library("ktor-cio", "io.ktor:ktor-client-cio:3.1.1")
                    library("ktor-json", "io.ktor:ktor-serialization-json:3.1.1")
                    library("unrelated-lib", "com.example:unrelated:1.0.0")
                }
                bundles {
                    bundle("web") { libraries("ktor-core", "ktor-cio", "ktor-json") }
                }
                distributions {
                    distribution("web-only") {
                        spec { byBundles { include("web") } }
                    }
                }
            }.process(distribution = "web-only")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("ktor-core", "ktor-cio", "ktor-json")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("unrelated-lib")
        }

        @Test
        fun `exclude by bundle removes bundle member libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                    library("ktor-cio", "io.ktor:ktor-client-cio:3.1.1")
                    library("keep-me", "com.example:keep:1.0.0")
                }
                bundles {
                    bundle("ktor") { libraries("ktor-core", "ktor-cio") }
                }
                distributions {
                    distribution("no-ktor") {
                        spec { byBundles { exclude("ktor") } }
                    }
                }
            }.process(distribution = "no-ktor")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys).containsExactly("keep-me")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("ktor-core")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("ktor-cio")
        }

        @Test
        fun `bundle filter with extends resolves inherited libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("ktor-core", "io.ktor:ktor-client-core:3.1.1")
                    library("ktor-cio", "io.ktor:ktor-client-cio:3.1.1")
                    library("ktor-auth", "io.ktor:ktor-client-auth:3.1.1")
                    library("unrelated", "com.example:unrelated:1.0.0")
                }
                bundles {
                    bundle("ktor-base") { libraries("ktor-core", "ktor-cio") }
                    bundle("ktor-full") {
                        libraries("ktor-auth")
                        extends("ktor-base")
                    }
                }
                distributions {
                    distribution("full-ktor") {
                        spec { byBundles { include("ktor-full") } }
                    }
                }
            }.process(distribution = "full-ktor")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("ktor-core", "ktor-cio", "ktor-auth")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("unrelated")
        }
    }

    @Nested
    inner class `Deprecated filtering` {

        @Test
        fun `exclude deprecated removes deprecated libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("active-lib", "com.example:active:2.0.0")
                    library("another-active", "com.example:another:1.0.0")
                    library("old-lib", "com.example:old-lib:0.9.0") {
                        deprecated(replacedBy = "active-lib", message = "Use active-lib")
                    }
                }
                distributions {
                    distribution("no-deprecated") {
                        spec { byDeprecated { exclude() } }
                    }
                }
            }.process(distribution = "no-deprecated")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("active-lib", "another-active")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("old-lib")
        }

        @Test
        fun `include deprecated keeps all libraries`() = runTest {
            val result = dependanger {
                libraries {
                    library("active-lib", "com.example:active:2.0.0")
                    library("another-active", "com.example:another:1.0.0")
                    library("old-lib", "com.example:old-lib:0.9.0") {
                        deprecated(replacedBy = "active-lib", message = "Use active-lib")
                    }
                }
                distributions {
                    distribution("with-deprecated") {
                        spec { byDeprecated { include() } }
                    }
                }
            }.process(distribution = "with-deprecated")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("active-lib", "another-active", "old-lib")
        }
    }

    @Nested
    inner class `Combined filters` {

        @Test
        fun `tag and group filters intersect`() = runTest {
            val result = dependanger {
                libraries {
                    library("spring-web", "org.springframework:spring-web:6.1.0") { tags("backend") }
                    library("spring-test", "org.springframework:spring-test:6.1.0") { tags("test") }
                    library("ktor-server", "io.ktor:ktor-server-core:3.1.1") { tags("backend") }
                }
                distributions {
                    distribution("spring-backend") {
                        spec {
                            byTags { include { anyOf("backend") } }
                            byGroups { include { matching("org.springframework*") } }
                        }
                    }
                }
            }.process(distribution = "spring-backend")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys).containsExactly("spring-web")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("spring-test")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("ktor-server")
        }

        @Test
        fun `tag include with alias exclude`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.a:lib:1.0") { tags("backend") }
                    library("lib-b", "com.b:lib:1.0") { tags("backend") }
                    library("lib-c", "com.c:lib:1.0") { tags("frontend") }
                }
                distributions {
                    distribution("backend-no-b") {
                        spec {
                            byTags { include { anyOf("backend") } }
                            byAliases { exclude("lib-b") }
                        }
                    }
                }
            }.process(distribution = "backend-no-b")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys).containsExactly("lib-a")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("lib-b")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("lib-c")
        }

        @Test
        fun `all filter types combined`() = runTest {
            val result = dependanger {
                libraries {
                    library("spring-web", "org.springframework:spring-web:6.1.0") { tags("backend", "web") }
                    library("spring-internal", "org.springframework.internal:debug:1.0.0") { tags("backend") }
                    library("legacy-spring", "org.springframework:spring-legacy:4.0.0") {
                        tags("backend")
                        deprecated(replacedBy = "spring-web", message = "Outdated")
                    }
                    library("ktor-server", "io.ktor:ktor-server-core:3.1.1") { tags("backend") }
                    library("excluded-alias", "org.springframework:spring-excluded:6.1.0") { tags("backend") }
                }
                distributions {
                    distribution("strict") {
                        spec {
                            byTags { include { anyOf("backend") } }
                            byGroups {
                                include { matching("org.springframework*") }
                                exclude { matching("org.springframework.internal*") }
                            }
                            byAliases { exclude("excluded-alias") }
                            byDeprecated { exclude() }
                        }
                    }
                }
            }.process(distribution = "strict")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys).containsExactly("spring-web")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("spring-internal")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("legacy-spring")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("ktor-server")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("excluded-alias")
        }
    }

    @Nested
    inner class `Tag filter with allOf` {

        @Test
        fun `allOf requires all tags present`() = runTest {
            val result = dependanger {
                libraries {
                    library("both-tags", "com.a:both:1.0") { tags("backend", "java") }
                    library("only-backend", "com.b:backend:1.0") { tags("backend") }
                    library("only-java", "com.c:java:1.0") { tags("java") }
                }
                distributions {
                    distribution("backend-java") {
                        spec { byTags { include { allOf("backend", "java") } } }
                    }
                }
            }.process(distribution = "backend-java")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys).containsExactly("both-tags")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("only-backend")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("only-java")
        }

        @Test
        fun `anyOf and allOf combined`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib-a", "com.a:lib:1.0") { tags("a") }
                    library("lib-bc", "com.b:lib:1.0") { tags("b", "c") }
                    library("lib-b-only", "com.c:lib:1.0") { tags("b") }
                    library("lib-none", "com.d:lib:1.0") { tags("x") }
                }
                distributions {
                    distribution("complex-tags") {
                        spec {
                            byTags {
                                include { anyOf("a") }
                                include { allOf("b", "c") }
                            }
                        }
                    }
                }
            }.process(distribution = "complex-tags")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.libraries.keys)
                .containsExactlyInAnyOrder("lib-a", "lib-bc")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("lib-b-only")
            assertThat((result as DependangerResult.Success).effective.libraries).doesNotContainKey("lib-none")
        }
    }

    @Nested
    inner class `Plugin filtering by distribution` {

        @Test
        fun `plugins are filtered by distribution tag rules`() = runTest {
            val result = dependanger {
                plugins {
                    plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm:2.1.20") { tags("jvm") }
                    plugin("kotlin-multiplatform", "org.jetbrains.kotlin.multiplatform:2.1.20") { tags("multiplatform") }
                    plugin("shadow", "com.github.johnrengelman.shadow:8.1.1") { tags("jvm") }
                }
                distributions {
                    distribution("jvm-only") {
                        pluginSpec { byTags { include { anyOf("jvm") } } }
                    }
                }
            }.process(distribution = "jvm-only")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.plugins.keys)
                .containsExactlyInAnyOrder("kotlin-jvm", "shadow")
            assertThat((result as DependangerResult.Success).effective.plugins).doesNotContainKey("kotlin-multiplatform")
        }

        @Test
        fun `plugins without tags are excluded when tag include is specified`() = runTest {
            val result = dependanger {
                plugins {
                    plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm:2.1.20") { tags("jvm") }
                    plugin("untagged-plugin", "com.example.untagged:1.0.0")
                }
                distributions {
                    distribution("tagged-only") {
                        pluginSpec { byTags { include { anyOf("jvm") } } }
                    }
                }
            }.process(distribution = "tagged-only")

            assertThat(result.isSuccess).isTrue()
            assertThat((result as DependangerResult.Success).effective.plugins.keys).containsExactly("kotlin-jvm")
            assertThat((result as DependangerResult.Success).effective.plugins).doesNotContainKey("untagged-plugin")
        }
    }
}
