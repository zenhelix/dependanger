package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.VersionReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConstraintApplicationTest {

    @Nested
    inner class `version constraint changes version in tree` {

        @Test
        fun `matching node version is replaced`() {
            val trees = listOf(
                tree("org.lib", "commons", "1.0"),
            )
            val constraints = listOf(
                Constraint.VersionConstraintDef(
                    coordinates = "org.lib:commons",
                    version = VersionReference.Literal("2.0"),
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("2.0")
        }

        @Test
        fun `non-matching nodes are left unchanged`() {
            val trees = listOf(
                tree("org.lib", "commons", "1.0"),
                tree("org.other", "utils", "3.0"),
            )
            val constraints = listOf(
                Constraint.VersionConstraintDef(
                    coordinates = "org.lib:commons",
                    version = VersionReference.Literal("2.0"),
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result[0].version).isEqualTo("2.0")
            assertThat(result[1].version).isEqualTo("3.0")
        }

        @Test
        fun `non-literal version reference leaves tree unchanged`() {
            val trees = listOf(
                tree("org.lib", "commons", "1.0"),
            )
            val constraints = listOf(
                Constraint.VersionConstraintDef(
                    coordinates = "org.lib:commons",
                    version = VersionReference.Reference("some-ref"),
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result[0].version).isEqualTo("1.0")
        }
    }

    @Nested
    inner class `exclude constraint removes matching subtrees` {

        @Test
        fun `top-level matching tree is removed`() {
            val trees = listOf(
                tree("org.lib", "commons", "1.0"),
                tree("org.lib", "utils", "2.0"),
            )
            val constraints = listOf(
                Constraint.Exclude(group = "org.lib", artifact = "commons")
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result).hasSize(1)
            assertThat(result[0].artifact).isEqualTo("utils")
        }

        @Test
        fun `nested matching child is removed`() {
            val trees = listOf(
                tree(
                    "root", "app", "1.0", children = listOf(
                        tree("org.lib", "commons", "1.0"),
                        tree("org.lib", "utils", "2.0"),
                    )
                ),
            )
            val constraints = listOf(
                Constraint.Exclude(group = "org.lib", artifact = "commons")
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result).hasSize(1)
            assertThat(result[0].children).hasSize(1)
            assertThat(result[0].children[0].artifact).isEqualTo("utils")
        }
    }

    @Nested
    inner class `substitute constraint replaces coordinates` {

        @Test
        fun `matching node group and artifact are replaced`() {
            val trees = listOf(
                tree("org.old", "lib", "1.0"),
            )
            val constraints = listOf(
                Constraint.Substitute(
                    from = "org.old:lib",
                    to = "org.new:lib-renamed",
                    because = "migration",
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result[0].group).isEqualTo("org.new")
            assertThat(result[0].artifact).isEqualTo("lib-renamed")
            assertThat(result[0].version).isEqualTo("1.0")
        }

        @Test
        fun `substitute with version replaces version too`() {
            val trees = listOf(
                tree("org.old", "lib", "1.0"),
            )
            val constraints = listOf(
                Constraint.Substitute(
                    from = "org.old:lib",
                    to = "org.new:lib-renamed:5.0",
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result[0].group).isEqualTo("org.new")
            assertThat(result[0].artifact).isEqualTo("lib-renamed")
            assertThat(result[0].version).isEqualTo("5.0")
        }

        @Test
        fun `nested matching child is substituted`() {
            val trees = listOf(
                tree(
                    "root", "app", "1.0", children = listOf(
                        tree("org.old", "lib", "1.0"),
                    )
                ),
            )
            val constraints = listOf(
                Constraint.Substitute(
                    from = "org.old:lib",
                    to = "org.new:lib-v2",
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            val child = result[0].children[0]
            assertThat(child.group).isEqualTo("org.new")
            assertThat(child.artifact).isEqualTo("lib-v2")
        }
    }

    @Nested
    inner class `invalid coordinate format is handled gracefully` {

        @Test
        fun `version constraint with invalid coordinates returns trees unchanged`() {
            val trees = listOf(
                tree("org.lib", "commons", "1.0"),
            )
            val constraints = listOf(
                Constraint.VersionConstraintDef(
                    coordinates = "invalid-no-colon",
                    version = VersionReference.Literal("9.0"),
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("1.0")
        }

        @Test
        fun `substitute with invalid from coordinate returns trees unchanged`() {
            val trees = listOf(
                tree("org.lib", "commons", "1.0"),
            )
            val constraints = listOf(
                Constraint.Substitute(
                    from = "no-colon",
                    to = "org.new:lib",
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result).hasSize(1)
            assertThat(result[0].group).isEqualTo("org.lib")
        }

        @Test
        fun `substitute with invalid to coordinate returns trees unchanged`() {
            val trees = listOf(
                tree("org.lib", "commons", "1.0"),
            )
            val constraints = listOf(
                Constraint.Substitute(
                    from = "org.lib:commons",
                    to = "no-colon",
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result).hasSize(1)
            assertThat(result[0].group).isEqualTo("org.lib")
        }
    }

    @Nested
    inner class `nested tree constraints applied recursively` {

        @Test
        fun `version constraint applies to deeply nested nodes`() {
            val trees = listOf(
                tree(
                    "root", "app", "1.0", children = listOf(
                        tree(
                            "mid", "layer", "1.0", children = listOf(
                                tree("org.lib", "deep", "1.0"),
                            )
                        ),
                    )
                ),
            )
            val constraints = listOf(
                Constraint.VersionConstraintDef(
                    coordinates = "org.lib:deep",
                    version = VersionReference.Literal("9.0"),
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            val deepNode = result[0].children[0].children[0]
            assertThat(deepNode.version).isEqualTo("9.0")
        }

        @Test
        fun `exclude removes deeply nested matching nodes`() {
            val trees = listOf(
                tree(
                    "root", "app", "1.0", children = listOf(
                        tree(
                            "mid", "layer", "1.0", children = listOf(
                                tree("org.lib", "to-remove", "1.0"),
                                tree("org.lib", "to-keep", "2.0"),
                            )
                        ),
                    )
                ),
            )
            val constraints = listOf(
                Constraint.Exclude(group = "org.lib", artifact = "to-remove")
            )

            val result = ConstraintApplier.apply(trees, constraints)

            val midChildren = result[0].children[0].children
            assertThat(midChildren).hasSize(1)
            assertThat(midChildren[0].artifact).isEqualTo("to-keep")
        }
    }

}
