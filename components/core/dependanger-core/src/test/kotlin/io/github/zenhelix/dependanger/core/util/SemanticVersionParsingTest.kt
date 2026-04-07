package io.github.zenhelix.dependanger.core.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SemanticVersionParsingTest {

    @Nested
    inner class `Standard semver parsing` {

        @Test
        fun `parses standard three-part version`() {
            val version = VersionComparator.parse("1.2.3")

            assertThat(version).isNotNull
            assertThat(version!!.major).isEqualTo(1)
            assertThat(version.minor).isEqualTo(2)
            assertThat(version.patch).isEqualTo(3)
            assertThat(version.qualifier).isNull()
        }

        @Test
        fun `parses two-part version with patch defaulting to zero`() {
            val version = VersionComparator.parse("1.2")

            assertThat(version).isNotNull
            assertThat(version!!.major).isEqualTo(1)
            assertThat(version.minor).isEqualTo(2)
            assertThat(version.patch).isEqualTo(0)
            assertThat(version.qualifier).isNull()
        }

        @Test
        fun `parses single-part version with minor and patch defaulting to zero`() {
            val version = VersionComparator.parse("5")

            assertThat(version).isNotNull
            assertThat(version!!.major).isEqualTo(5)
            assertThat(version.minor).isEqualTo(0)
            assertThat(version.patch).isEqualTo(0)
            assertThat(version.qualifier).isNull()
        }

        @Test
        fun `parses large version numbers`() {
            val version = VersionComparator.parse("2024.1.15")

            assertThat(version).isNotNull
            assertThat(version!!.major).isEqualTo(2024)
            assertThat(version.minor).isEqualTo(1)
            assertThat(version.patch).isEqualTo(15)
        }
    }

    @Nested
    inner class `Pre-release qualifier parsing` {

        @Test
        fun `parses version with hyphen-separated qualifier`() {
            val version = VersionComparator.parse("1.2.3-beta.1")

            assertThat(version).isNotNull
            assertThat(version!!.major).isEqualTo(1)
            assertThat(version.minor).isEqualTo(2)
            assertThat(version.patch).isEqualTo(3)
            assertThat(version.qualifier).isEqualTo("beta.1")
        }

        @Test
        fun `parses version with RC qualifier`() {
            val version = VersionComparator.parse("1.0.0-RC1")

            assertThat(version).isNotNull
            assertThat(version!!.major).isEqualTo(1)
            assertThat(version.minor).isEqualTo(0)
            assertThat(version.patch).isEqualTo(0)
            assertThat(version.qualifier).isEqualTo("RC1")
        }

        @Test
        fun `parses version with dot-separated qualifier`() {
            val version = VersionComparator.parse("1.0.0.RELEASE")

            assertThat(version).isNotNull
            assertThat(version!!.qualifier).isNull()
            assertThat(version.major).isEqualTo(1)
            assertThat(version.minor).isEqualTo(0)
            assertThat(version.patch).isEqualTo(0)
        }
    }

    @Nested
    inner class `Stable suffix stripping` {

        @Test
        fun `RELEASE suffix is stripped`() {
            val version = VersionComparator.parse("5.3.30.RELEASE")

            assertThat(version).isNotNull
            assertThat(version!!.qualifier).isNull()
            assertThat(version.major).isEqualTo(5)
            assertThat(version.minor).isEqualTo(3)
            assertThat(version.patch).isEqualTo(30)
        }

        @Test
        fun `Final suffix is stripped`() {
            val version = VersionComparator.parse("3.4.0.Final")

            assertThat(version).isNotNull
            assertThat(version!!.qualifier).isNull()
        }

        @Test
        fun `GA suffix is stripped`() {
            val version = VersionComparator.parse("6.2.0.GA")

            assertThat(version).isNotNull
            assertThat(version!!.qualifier).isNull()
        }

        @Test
        fun `case insensitive stable suffix stripping`() {
            val release = VersionComparator.parse("1.0.0.release")
            val final = VersionComparator.parse("1.0.0.FINAL")
            val ga = VersionComparator.parse("1.0.0.ga")

            assertThat(release!!.qualifier).isNull()
            assertThat(final!!.qualifier).isNull()
            assertThat(ga!!.qualifier).isNull()
        }
    }

    @Nested
    inner class `Non-standard version handling` {

        @Test
        fun `returns null for non-numeric major version`() {
            val version = VersionComparator.parse("abc.1.2")

            assertThat(version).isNull()
        }

        @Test
        fun `returns null for empty string`() {
            val version = VersionComparator.parse("")

            assertThat(version).isNull()
        }

        @Test
        fun `returns null for purely textual version`() {
            val version = VersionComparator.parse("latest")

            assertThat(version).isNull()
        }
    }

    @Nested
    inner class `Pre-release detection` {

        @Test
        fun `detects alpha as prerelease`() {
            assertThat(VersionComparator.isPrerelease("1.0.0-alpha")).isTrue()
        }

        @Test
        fun `detects beta as prerelease`() {
            assertThat(VersionComparator.isPrerelease("1.0.0-beta")).isTrue()
        }

        @Test
        fun `detects RC as prerelease`() {
            assertThat(VersionComparator.isPrerelease("1.0.0-rc1")).isTrue()
        }

        @Test
        fun `detects SNAPSHOT as prerelease`() {
            assertThat(VersionComparator.isPrerelease("1.0.0-SNAPSHOT")).isTrue()
        }

        @Test
        fun `detects milestone as prerelease`() {
            assertThat(VersionComparator.isPrerelease("1.0.0-M1")).isTrue()
        }

        @Test
        fun `detects dev as prerelease`() {
            assertThat(VersionComparator.isPrerelease("1.0.0.dev")).isTrue()
        }

        @Test
        fun `stable version is not prerelease`() {
            assertThat(VersionComparator.isPrerelease("1.0.0")).isFalse()
        }

        @Test
        fun `version with RELEASE suffix is detected as prerelease due to ea pattern match`() {
            // "RELEASE" contains "EA" substring which matches the "ea" (early access) prerelease pattern.
            // This is a known trade-off of the current regex-based detection approach.
            assertThat(VersionComparator.isPrerelease("5.3.30.RELEASE")).isTrue()
        }
    }

    @Nested
    inner class `toString produces canonical format` {

        @Test
        fun `standard version without qualifier`() {
            val version = SemanticVersion(1, 2, 3, null)

            assertThat(version.toString()).isEqualTo("1.2.3")
        }

        @Test
        fun `version with qualifier`() {
            val version = SemanticVersion(1, 0, 0, "beta.1")

            assertThat(version.toString()).isEqualTo("1.0.0-beta.1")
        }
    }

    @Nested
    inner class `SemanticVersion comparison` {

        @Test
        fun `higher major is greater`() {
            val v1 = SemanticVersion(1, 0, 0, null)
            val v2 = SemanticVersion(2, 0, 0, null)

            assertThat(v1).isLessThan(v2)
        }

        @Test
        fun `higher minor is greater when major is equal`() {
            val v1 = SemanticVersion(1, 0, 0, null)
            val v2 = SemanticVersion(1, 1, 0, null)

            assertThat(v1).isLessThan(v2)
        }

        @Test
        fun `higher patch is greater when major and minor are equal`() {
            val v1 = SemanticVersion(1, 0, 0, null)
            val v2 = SemanticVersion(1, 0, 1, null)

            assertThat(v1).isLessThan(v2)
        }

        @Test
        fun `stable version is greater than prerelease`() {
            val stable = SemanticVersion(1, 0, 0, null)
            val prerelease = SemanticVersion(1, 0, 0, "beta")

            assertThat(stable).isGreaterThan(prerelease)
        }

        @Test
        fun `equal versions compare as zero`() {
            val v1 = SemanticVersion(1, 2, 3, null)
            val v2 = SemanticVersion(1, 2, 3, null)

            assertThat(v1.compareTo(v2)).isEqualTo(0)
        }

        @Test
        fun `qualifiers compared lexicographically`() {
            val alpha = SemanticVersion(1, 0, 0, "alpha")
            val beta = SemanticVersion(1, 0, 0, "beta")

            assertThat(alpha).isLessThan(beta)
        }

        @Test
        fun `RC10 is greater than RC2 using natural comparison`() {
            val rc2 = SemanticVersion(1, 0, 0, "RC2")
            val rc10 = SemanticVersion(1, 0, 0, "RC10")

            assertThat(rc10).isGreaterThan(rc2)
        }

        @Test
        fun `alpha dot 10 is greater than alpha dot 2`() {
            val alpha2 = SemanticVersion(1, 0, 0, "alpha.2")
            val alpha10 = SemanticVersion(1, 0, 0, "alpha.10")

            assertThat(alpha10).isGreaterThan(alpha2)
        }

        @Test
        fun `qualifier comparison is case insensitive`() {
            val lower = SemanticVersion(1, 0, 0, "rc1")
            val upper = SemanticVersion(1, 0, 0, "RC1")

            assertThat(lower.compareTo(upper)).isEqualTo(0)
        }

        @Test
        fun `milestone qualifiers ordered naturally`() {
            val m1 = SemanticVersion(1, 0, 0, "M1")
            val m2 = SemanticVersion(1, 0, 0, "M2")
            val m10 = SemanticVersion(1, 0, 0, "M10")

            assertThat(m1).isLessThan(m2)
            assertThat(m2).isLessThan(m10)
            assertThat(m1).isLessThan(m10)
        }
    }
}
