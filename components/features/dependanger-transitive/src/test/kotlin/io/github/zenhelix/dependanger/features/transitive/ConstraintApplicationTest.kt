package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
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
                    coordinate = MavenCoordinate("org.lib", "commons"),
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
                    coordinate = MavenCoordinate("org.lib", "commons"),
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
                    coordinate = MavenCoordinate("org.lib", "commons"),
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
                Constraint.Exclude(coordinate = MavenCoordinate("org.lib", "commons"))
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result).hasSize(1)
            assertThat(result[0].coordinate.artifact).isEqualTo("utils")
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
                Constraint.Exclude(coordinate = MavenCoordinate("org.lib", "commons"))
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result).hasSize(1)
            assertThat(result[0].children).hasSize(1)
            assertThat(result[0].children[0].coordinate.artifact).isEqualTo("utils")
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
                    from = MavenCoordinate("org.old", "lib"),
                    to = MavenCoordinate("org.new", "lib-renamed"),
                    toVersion = null,
                    because = "migration",
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result[0].coordinate.group).isEqualTo("org.new")
            assertThat(result[0].coordinate.artifact).isEqualTo("lib-renamed")
            assertThat(result[0].version).isEqualTo("1.0")
        }

        @Test
        fun `substitute with version replaces version too`() {
            val trees = listOf(
                tree("org.old", "lib", "1.0"),
            )
            val constraints = listOf(
                Constraint.Substitute(
                    from = MavenCoordinate("org.old", "lib"),
                    to = MavenCoordinate("org.new", "lib-renamed"),
                    toVersion = "5.0",
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            assertThat(result[0].coordinate.group).isEqualTo("org.new")
            assertThat(result[0].coordinate.artifact).isEqualTo("lib-renamed")
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
                    from = MavenCoordinate("org.old", "lib"),
                    to = MavenCoordinate("org.new", "lib-v2"),
                    toVersion = null,
                    because = null,
                )
            )

            val result = ConstraintApplier.apply(trees, constraints)

            val child = result[0].children[0]
            assertThat(child.coordinate.group).isEqualTo("org.new")
            assertThat(child.coordinate.artifact).isEqualTo("lib-v2")
        }
    }

    @Nested
    inner class `per-library scoped constraints apply to subtree only` {

        @Test
        fun `exclude removes child but not root`() {
            val root = tree(
                "io.ktor", "ktor-client", "2.0", children = listOf(
                    tree("org.slf4j", "slf4j-api", "1.7"),
                    tree("org.jetbrains", "annotations", "24.0"),
                )
            )
            val constraints = listOf(
                Constraint.Exclude(coordinate = MavenCoordinate("org.slf4j", "slf4j-api"))
            )

            val result = ConstraintApplier.applyToChildren(root, constraints)

            assertThat(result.coordinate.group).isEqualTo("io.ktor")
            assertThat(result.children).hasSize(1)
            assertThat(result.children[0].coordinate.artifact).isEqualTo("annotations")
        }

        @Test
        fun `exclude matching root coordinate does not remove root`() {
            val root = tree(
                "io.ktor", "ktor-client", "2.0", children = listOf(
                    tree("org.slf4j", "slf4j-api", "1.7"),
                )
            )
            val constraints = listOf(
                Constraint.Exclude(coordinate = MavenCoordinate("io.ktor", "ktor-client"))
            )

            val result = ConstraintApplier.applyToChildren(root, constraints)

            assertThat(result.coordinate.group).isEqualTo("io.ktor")
            assertThat(result.coordinate.artifact).isEqualTo("ktor-client")
            assertThat(result.children).hasSize(1)
        }

        @Test
        fun `version constraint applied to nested child`() {
            val root = tree(
                "io.ktor", "ktor-client", "2.0", children = listOf(
                    tree("org.slf4j", "slf4j-api", "1.7"),
                )
            )
            val constraints = listOf(
                Constraint.VersionConstraintDef(
                    coordinate = MavenCoordinate("org.slf4j", "slf4j-api"),
                    version = VersionReference.Literal("2.0"),
                    because = "upgrade",
                )
            )

            val result = ConstraintApplier.applyToChildren(root, constraints)

            assertThat(result.children[0].version).isEqualTo("2.0")
        }

        @Test
        fun `substitute applied to child`() {
            val root = tree(
                "io.ktor", "ktor-client", "2.0", children = listOf(
                    tree("javax.annotation", "javax.annotation-api", "1.3"),
                )
            )
            val constraints = listOf(
                Constraint.Substitute(
                    from = MavenCoordinate("javax.annotation", "javax.annotation-api"),
                    to = MavenCoordinate("jakarta.annotation", "jakarta.annotation-api"),
                    toVersion = "2.0",
                    because = "jakarta migration",
                )
            )

            val result = ConstraintApplier.applyToChildren(root, constraints)

            val child = result.children[0]
            assertThat(child.coordinate.group).isEqualTo("jakarta.annotation")
            assertThat(child.coordinate.artifact).isEqualTo("jakarta.annotation-api")
            assertThat(child.version).isEqualTo("2.0")
        }

        @Test
        fun `empty constraints returns tree unchanged`() {
            val root = tree(
                "io.ktor", "ktor-client", "2.0", children = listOf(
                    tree("org.slf4j", "slf4j-api", "1.7"),
                )
            )

            val result = ConstraintApplier.applyToChildren(root, emptyList())

            assertThat(result).isEqualTo(root)
        }

        @Test
        fun `deeply nested exclude works`() {
            val root = tree(
                "io.ktor", "ktor-client", "2.0", children = listOf(
                    tree(
                        "io.ktor", "ktor-io", "2.0", children = listOf(
                            tree("org.slf4j", "slf4j-api", "1.7"),
                            tree("org.jetbrains", "annotations", "24.0"),
                        )
                    ),
                )
            )
            val constraints = listOf(
                Constraint.Exclude(coordinate = MavenCoordinate("org.slf4j", "slf4j-api"))
            )

            val result = ConstraintApplier.applyToChildren(root, constraints)

            assertThat(result.children).hasSize(1)
            assertThat(result.children[0].children).hasSize(1)
            assertThat(result.children[0].children[0].coordinate.artifact).isEqualTo("annotations")
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
                    coordinate = MavenCoordinate("org.lib", "deep"),
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
                Constraint.Exclude(coordinate = MavenCoordinate("org.lib", "to-remove"))
            )

            val result = ConstraintApplier.apply(trees, constraints)

            val midChildren = result[0].children[0].children
            assertThat(midChildren).hasSize(1)
            assertThat(midChildren[0].coordinate.artifact).isEqualTo("to-keep")
        }
    }

}
