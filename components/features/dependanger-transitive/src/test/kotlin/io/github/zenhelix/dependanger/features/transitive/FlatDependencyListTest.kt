package io.github.zenhelix.dependanger.features.transitive

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FlatDependencyListTest {

    @Nested
    inner class `simple tree flattens to list preserving order` {

        @Test
        fun `parent appears before its children`() {
            val trees = listOf(
                tree(
                    "com.root", "app", "1.0", children = listOf(
                        tree("org.lib", "a", "2.0"),
                        tree("org.lib", "b", "3.0"),
                    )
                ),
            )

            val result = FlatListBuilder.build(trees, emptyList())

            assertThat(result.map { "${it.coordinate}" })
                .containsExactly("com.root:app", "org.lib:a", "org.lib:b")
        }

        @Test
        fun `deeply nested nodes are included in depth-first order`() {
            val trees = listOf(
                tree(
                    "root", "app", "1.0", children = listOf(
                        tree(
                            "mid", "layer", "1.0", children = listOf(
                                tree("deep", "leaf", "1.0"),
                            )
                        ),
                    )
                ),
            )

            val result = FlatListBuilder.build(trees, emptyList())

            assertThat(result).hasSize(3)
            assertThat(result.map { it.coordinate.artifact })
                .containsExactly("app", "layer", "leaf")
        }
    }

    @Nested
    inner class `duplicate coordinates are deduplicated` {

        @Test
        fun `first occurrence wins`() {
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

            val result = FlatListBuilder.build(trees, emptyList())

            val commons = result.filter { it.coordinate.artifact == "commons" }
            assertThat(commons).hasSize(1)
            assertThat(commons[0].version).isEqualTo("1.0")
        }
    }

    @Nested
    inner class `cycle nodes are skipped` {

        @Test
        fun `tree marked as cycle is not included in flat list`() {
            val trees = listOf(
                tree("org.lib", "a", "1.0"),
                tree("org.lib", "b", "2.0", isCycle = true),
            )

            val result = FlatListBuilder.build(trees, emptyList())

            assertThat(result).hasSize(1)
            assertThat(result[0].coordinate.artifact).isEqualTo("a")
        }

        @Test
        fun `children of cycle node are not traversed`() {
            val trees = listOf(
                tree(
                    "org.lib", "a", "1.0", isCycle = true, children = listOf(
                        tree("org.lib", "child", "1.0"),
                    )
                ),
            )

            val result = FlatListBuilder.build(trees, emptyList())

            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class `nodes without version are skipped` {

        @Test
        fun `null version node is excluded from flat list`() {
            val trees = listOf(
                tree("org.lib", "a", null),
                tree("org.lib", "b", "2.0"),
            )

            val result = FlatListBuilder.build(trees, emptyList())

            assertThat(result).hasSize(1)
            assertThat(result[0].coordinate.artifact).isEqualTo("b")
        }
    }

    @Nested
    inner class `direct dependencies are marked correctly` {

        @Test
        fun `library present in directLibraries is marked as direct`() {
            val trees = listOf(
                tree(
                    "org.lib", "direct-dep", "1.0", children = listOf(
                        tree("org.lib", "transitive-dep", "2.0"),
                    )
                ),
            )
            val directLibraries = listOf(
                effectiveLibrary("org.lib", "direct-dep"),
            )

            val result = FlatListBuilder.build(trees, directLibraries)

            val direct = result.first { it.coordinate.artifact == "direct-dep" }
            val transitive = result.first { it.coordinate.artifact == "transitive-dep" }
            assertThat(direct.isDirectDependency).isTrue()
            assertThat(transitive.isDirectDependency).isFalse()
        }

        @Test
        fun `empty directLibraries means nothing is marked direct`() {
            val trees = listOf(
                tree("org.lib", "a", "1.0"),
            )

            val result = FlatListBuilder.build(trees, emptyList())

            assertThat(result[0].isDirectDependency).isFalse()
        }
    }

    @Nested
    inner class `scope is preserved from tree` {

        @Test
        fun `scope value from tree node carries over to flat dependency`() {
            val trees = listOf(
                tree("org.lib", "a", "1.0", scope = "runtime"),
            )

            val result = FlatListBuilder.build(trees, emptyList())

            assertThat(result[0].scope).isEqualTo("runtime")
        }

        @Test
        fun `null scope is preserved`() {
            val trees = listOf(
                tree("org.lib", "a", "1.0", scope = null),
            )

            val result = FlatListBuilder.build(trees, emptyList())

            assertThat(result[0].scope).isNull()
        }
    }

}
