package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.features.transitive.ConflictResolutionStrategy
import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.VersionReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConflictDetectionTest {

    @Nested
    inner class `when two libraries depend on the same artifact with different versions` {

        @Test
        fun `conflict is detected`() {
            val trees = listOf(
                tree(
                    "com.example", "app", "1.0", children = listOf(
                        tree("org.lib", "commons", "1.0"),
                    )
                ),
                tree(
                    "com.example", "service", "2.0", children = listOf(
                        tree("org.lib", "commons", "2.0"),
                    )
                ),
            )

            val conflicts = ConflictDetector.detectConflicts(
                trees, emptyList(), ConflictResolutionStrategy.HIGHEST
            )

            assertThat(conflicts).hasSize(1)
            assertThat(conflicts[0].group).isEqualTo("org.lib")
            assertThat(conflicts[0].artifact).isEqualTo("commons")
            assertThat(conflicts[0].requestedVersions).containsExactly("1.0", "2.0")
        }

        @Test
        fun `multiple conflicting artifacts each produce a separate conflict`() {
            val trees = listOf(
                tree(
                    "root", "a", "1.0", children = listOf(
                        tree("org.lib", "x", "1.0"),
                        tree("org.lib", "y", "3.0"),
                    )
                ),
                tree(
                    "root", "b", "1.0", children = listOf(
                        tree("org.lib", "x", "2.0"),
                        tree("org.lib", "y", "4.0"),
                    )
                ),
            )

            val conflicts = ConflictDetector.detectConflicts(
                trees, emptyList(), ConflictResolutionStrategy.HIGHEST
            )

            assertThat(conflicts).hasSize(2)
            assertThat(conflicts.map { "${it.group}:${it.artifact}" })
                .containsExactlyInAnyOrder("org.lib:x", "org.lib:y")
        }
    }

    @Nested
    inner class `when all occurrences of an artifact have the same version` {

        @Test
        fun `no conflict is reported`() {
            val trees = listOf(
                tree(
                    "root", "a", "1.0", children = listOf(
                        tree("org.lib", "commons", "2.0"),
                    )
                ),
                tree(
                    "root", "b", "1.0", children = listOf(
                        tree("org.lib", "commons", "2.0"),
                    )
                ),
            )

            val conflicts = ConflictDetector.detectConflicts(
                trees, emptyList(), ConflictResolutionStrategy.HIGHEST
            )

            assertThat(conflicts).isEmpty()
        }
    }

    @Nested
    inner class `conflict resolution with HIGHEST strategy` {

        @Test
        fun `resolved version is the highest among conflicting versions`() {
            val trees = listOf(
                tree(
                    "root", "a", "1.0", children = listOf(
                        tree("org.lib", "commons", "1.2.0"),
                    )
                ),
                tree(
                    "root", "b", "1.0", children = listOf(
                        tree("org.lib", "commons", "2.0.0"),
                    )
                ),
            )

            val conflicts = ConflictDetector.detectConflicts(
                trees, emptyList(), ConflictResolutionStrategy.HIGHEST
            )

            assertThat(conflicts).hasSize(1)
            assertThat(conflicts[0].resolvedVersion).isEqualTo("2.0.0")
            assertThat(conflicts[0].resolution).isEqualTo(ConflictResolutionStrategy.HIGHEST)
        }
    }

    @Nested
    inner class `conflict resolution with FIRST strategy` {

        @Test
        fun `resolved version is the first encountered version`() {
            val trees = listOf(
                tree(
                    "root", "a", "1.0", children = listOf(
                        tree("org.lib", "commons", "1.0.0"),
                    )
                ),
                tree(
                    "root", "b", "1.0", children = listOf(
                        tree("org.lib", "commons", "3.0.0"),
                    )
                ),
            )

            val conflicts = ConflictDetector.detectConflicts(
                trees, emptyList(), ConflictResolutionStrategy.FIRST
            )

            assertThat(conflicts).hasSize(1)
            assertThat(conflicts[0].resolvedVersion).isEqualTo("1.0.0")
            assertThat(conflicts[0].resolution).isEqualTo(ConflictResolutionStrategy.FIRST)
        }
    }

    @Nested
    inner class `version constraint overrides conflict resolution` {

        @Test
        fun `constraint version is used instead of strategy result`() {
            val trees = listOf(
                tree(
                    "root", "a", "1.0", children = listOf(
                        tree("org.lib", "commons", "1.0"),
                    )
                ),
                tree(
                    "root", "b", "1.0", children = listOf(
                        tree("org.lib", "commons", "2.0"),
                    )
                ),
            )
            val constraints = listOf(
                Constraint.VersionConstraintDef(
                    coordinates = "org.lib:commons",
                    version = VersionReference.Literal("1.5"),
                    because = "pinned by policy",
                )
            )

            val conflicts = ConflictDetector.detectConflicts(
                trees, constraints, ConflictResolutionStrategy.HIGHEST
            )

            assertThat(conflicts).hasSize(1)
            assertThat(conflicts[0].resolvedVersion).isEqualTo("1.5")
            assertThat(conflicts[0].resolution).isEqualTo(ConflictResolutionStrategy.CONSTRAINT)
        }

        @Test
        fun `non-literal version reference falls back to strategy`() {
            val trees = listOf(
                tree(
                    "root", "a", "1.0", children = listOf(
                        tree("org.lib", "commons", "1.0"),
                    )
                ),
                tree(
                    "root", "b", "1.0", children = listOf(
                        tree("org.lib", "commons", "3.0"),
                    )
                ),
            )
            val constraints = listOf(
                Constraint.VersionConstraintDef(
                    coordinates = "org.lib:commons",
                    version = VersionReference.Reference("commons-version"),
                    because = null,
                )
            )

            val conflicts = ConflictDetector.detectConflicts(
                trees, constraints, ConflictResolutionStrategy.HIGHEST
            )

            assertThat(conflicts).hasSize(1)
            assertThat(conflicts[0].resolvedVersion).isEqualTo("3.0")
        }
    }

    @Nested
    inner class `cycle nodes are skipped during conflict detection` {

        @Test
        fun `cycle node version is not collected`() {
            val trees = listOf(
                tree(
                    "root", "a", "1.0", children = listOf(
                        tree("org.lib", "commons", "1.0"),
                        tree("org.lib", "commons", "1.0", isCycle = true),
                    )
                ),
                tree(
                    "root", "b", "1.0", children = listOf(
                        tree("org.lib", "commons", "2.0", isCycle = true),
                    )
                ),
            )

            val conflicts = ConflictDetector.detectConflicts(
                trees, emptyList(), ConflictResolutionStrategy.HIGHEST
            )

            // Only one non-cycle version "1.0" is collected, so no conflict
            assertThat(conflicts).isEmpty()
        }
    }

}
