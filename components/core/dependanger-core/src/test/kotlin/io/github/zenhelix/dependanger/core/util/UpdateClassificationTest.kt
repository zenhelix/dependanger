package io.github.zenhelix.dependanger.core.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UpdateClassificationTest {

    @Nested
    inner class `Update type classification` {

        @Test
        fun `patch update when only patch version increases`() {
            val current = SemanticVersion(1, 0, 0, null)
            val latest = SemanticVersion(1, 0, 1, null)

            val updateType = VersionComparator.classifyUpdate(current, latest)

            assertThat(updateType).isEqualTo(UpdateType.PATCH)
        }

        @Test
        fun `minor update when minor version increases`() {
            val current = SemanticVersion(1, 0, 0, null)
            val latest = SemanticVersion(1, 1, 0, null)

            val updateType = VersionComparator.classifyUpdate(current, latest)

            assertThat(updateType).isEqualTo(UpdateType.MINOR)
        }

        @Test
        fun `major update when major version increases`() {
            val current = SemanticVersion(1, 0, 0, null)
            val latest = SemanticVersion(2, 0, 0, null)

            val updateType = VersionComparator.classifyUpdate(current, latest)

            assertThat(updateType).isEqualTo(UpdateType.MAJOR)
        }

        @Test
        fun `prerelease when no numeric components increase`() {
            val current = SemanticVersion(1, 0, 0, "alpha")
            val latest = SemanticVersion(1, 0, 0, "beta")

            val updateType = VersionComparator.classifyUpdate(current, latest)

            assertThat(updateType).isEqualTo(UpdateType.PRERELEASE)
        }

        @Test
        fun `major takes precedence when all components increase`() {
            val current = SemanticVersion(1, 0, 0, null)
            val latest = SemanticVersion(2, 1, 1, null)

            val updateType = VersionComparator.classifyUpdate(current, latest)

            assertThat(updateType).isEqualTo(UpdateType.MAJOR)
        }

        @Test
        fun `minor takes precedence over patch increase`() {
            val current = SemanticVersion(1, 0, 0, null)
            val latest = SemanticVersion(1, 2, 5, null)

            val updateType = VersionComparator.classifyUpdate(current, latest)

            assertThat(updateType).isEqualTo(UpdateType.MINOR)
        }

        @Test
        fun `identical versions classified as prerelease`() {
            val current = SemanticVersion(1, 0, 0, null)
            val latest = SemanticVersion(1, 0, 0, null)

            val updateType = VersionComparator.classifyUpdate(current, latest)

            assertThat(updateType).isEqualTo(UpdateType.PRERELEASE)
        }
    }

    @Nested
    inner class `String-based version comparison` {

        @Test
        fun `compare returns positive when first version is higher`() {
            val result = VersionComparator.compare("2.0.0", "1.0.0")

            assertThat(result).isGreaterThan(0)
        }

        @Test
        fun `compare returns negative when first version is lower`() {
            val result = VersionComparator.compare("1.0.0", "2.0.0")

            assertThat(result).isLessThan(0)
        }

        @Test
        fun `compare returns zero for equal versions`() {
            val result = VersionComparator.compare("1.0.0", "1.0.0")

            assertThat(result).isEqualTo(0)
        }

        @Test
        fun `falls back to string comparison for unparseable versions`() {
            val result = VersionComparator.compare("abc", "def")

            assertThat(result).isLessThan(0)
        }
    }

    @Nested
    inner class `Selecting highest version` {

        @Test
        fun `selects highest from a list of versions`() {
            val versions = listOf("1.0.0", "2.1.0", "1.5.3", "2.0.0")

            val highest = VersionComparator.selectHighest(versions)

            assertThat(highest).isEqualTo("2.1.0")
        }

        @Test
        fun `returns null for empty list`() {
            val highest = VersionComparator.selectHighest(emptyList())

            assertThat(highest).isNull()
        }

        @Test
        fun `stable version selected over prerelease with same numbers`() {
            val versions = listOf("1.0.0-beta", "1.0.0")

            val highest = VersionComparator.selectHighest(versions)

            assertThat(highest).isEqualTo("1.0.0")
        }

        @Test
        fun `single element list returns that element`() {
            val highest = VersionComparator.selectHighest(listOf("3.2.1"))

            assertThat(highest).isEqualTo("3.2.1")
        }
    }

    @Nested
    inner class `Version component extraction` {

        @Test
        fun `parseMajor extracts major component`() {
            assertThat(VersionComparator.parseMajor("2.1.20")).isEqualTo("2")
        }

        @Test
        fun `parseMajorMinor extracts major and minor`() {
            assertThat(VersionComparator.parseMajorMinor("2.1.20")).isEqualTo("2.1")
        }

        @Test
        fun `parseMajor with single part version`() {
            assertThat(VersionComparator.parseMajor("5")).isEqualTo("5")
        }

        @Test
        fun `parseMajorMinor with single part version`() {
            assertThat(VersionComparator.parseMajorMinor("5")).isEqualTo("5")
        }
    }
}
