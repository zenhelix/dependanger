package io.github.zenhelix.dependanger.features.updates

import io.github.zenhelix.dependanger.core.util.GlobMatcher
import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.core.util.VersionComparator
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import io.github.zenhelix.dependanger.maven.client.model.MetadataFetchResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UpdateCheckBehaviorTest {

    @Nested
    inner class `When Maven metadata contains newer version` {

        @Test
        fun `update is detected when latest stable is higher than current`() {
            val currentVersion = "1.0.0"
            val availableVersions = listOf("1.0.0", "1.0.1", "1.1.0", "2.0.0")

            val current = VersionComparator.parse(currentVersion)!!
            val stableVersions = availableVersions.filterNot { VersionComparator.isPrerelease(it) }
            val latestStable = VersionComparator.selectHighest(stableVersions)
            val targetVersion = VersionComparator.parse(latestStable!!)!!

            assertThat(targetVersion).isGreaterThan(current)
            assertThat(latestStable).isEqualTo("2.0.0")
        }

        @Test
        fun `no update detected when current is the latest`() {
            val currentVersion = "2.0.0"
            val availableVersions = listOf("1.0.0", "1.5.0", "2.0.0")

            val current = VersionComparator.parse(currentVersion)!!
            val latestStable = VersionComparator.selectHighest(availableVersions)
            val targetVersion = VersionComparator.parse(latestStable!!)!!

            assertThat(targetVersion).isEqualByComparingTo(current)
        }
    }

    @Nested
    inner class `Update type classification from parsed versions` {

        @Test
        fun `PATCH update from 1_0_0 to 1_0_1`() {
            val current = VersionComparator.parse("1.0.0")!!
            val latest = VersionComparator.parse("1.0.1")!!

            assertThat(VersionComparator.classifyUpdate(current, latest)).isEqualTo(UpdateType.PATCH)
        }

        @Test
        fun `MINOR update from 1_0_0 to 1_1_0`() {
            val current = VersionComparator.parse("1.0.0")!!
            val latest = VersionComparator.parse("1.1.0")!!

            assertThat(VersionComparator.classifyUpdate(current, latest)).isEqualTo(UpdateType.MINOR)
        }

        @Test
        fun `MAJOR update from 1_0_0 to 2_0_0`() {
            val current = VersionComparator.parse("1.0.0")!!
            val latest = VersionComparator.parse("2.0.0")!!

            assertThat(VersionComparator.classifyUpdate(current, latest)).isEqualTo(UpdateType.MAJOR)
        }

        @Test
        fun `PRERELEASE update when only qualifier changes`() {
            val current = VersionComparator.parse("1.0.0-alpha")!!
            val latest = VersionComparator.parse("1.0.0-beta")!!

            assertThat(VersionComparator.classifyUpdate(current, latest)).isEqualTo(UpdateType.PRERELEASE)
        }

        @Test
        fun `real-world Spring Boot patch update`() {
            val current = VersionComparator.parse("3.4.0")!!
            val latest = VersionComparator.parse("3.4.3")!!

            assertThat(VersionComparator.classifyUpdate(current, latest)).isEqualTo(UpdateType.PATCH)
        }

        @Test
        fun `real-world Kotlin minor update`() {
            val current = VersionComparator.parse("2.0.21")!!
            val latest = VersionComparator.parse("2.1.20")!!

            assertThat(VersionComparator.classifyUpdate(current, latest)).isEqualTo(UpdateType.MINOR)
        }
    }

    @Nested
    inner class `Exclude patterns filter out ignored libraries` {

        @Test
        fun `library matching glob exclude pattern is filtered out`() {
            val excludePatterns = listOf("org.springframework*:*")
            val group = "org.springframework.boot"
            val artifact = "spring-boot-starter"

            val isExcluded = excludePatterns.any { pattern ->
                GlobMatcher.matches(pattern, group, artifact)
            }

            assertThat(isExcluded).isTrue()
        }

        @Test
        fun `library not matching any exclude pattern passes through`() {
            val excludePatterns = listOf("org.springframework*:*")
            val group = "org.jetbrains.kotlin"
            val artifact = "kotlin-stdlib"

            val isExcluded = excludePatterns.any { pattern ->
                GlobMatcher.matches(pattern, group, artifact)
            }

            assertThat(isExcluded).isFalse()
        }

        @Test
        fun `exact artifact exclude pattern matches only that artifact`() {
            val excludePatterns = listOf("com.example:legacy-lib")

            val matchesTarget = excludePatterns.any { pattern ->
                GlobMatcher.matches(pattern, "com.example", "legacy-lib")
            }
            val matchesOther = excludePatterns.any { pattern ->
                GlobMatcher.matches(pattern, "com.example", "other-lib")
            }

            assertThat(matchesTarget).isTrue()
            assertThat(matchesOther).isFalse()
        }
    }

    @Nested
    inner class `Pre-release version handling` {

        @Test
        fun `when includePrerelease is false only stable versions are considered`() {
            val allVersions = listOf("1.0.0", "1.1.0-beta", "1.1.0-RC1", "1.0.1")
            val includePrerelease = false

            val stableVersions = allVersions.filterNot { VersionComparator.isPrerelease(it) }
            val target = if (includePrerelease) {
                VersionComparator.selectHighest(allVersions)
            } else {
                VersionComparator.selectHighest(stableVersions)
            }

            assertThat(target).isEqualTo("1.0.1")
        }

        @Test
        fun `when includePrerelease is true prerelease versions are considered`() {
            val allVersions = listOf("1.0.0", "1.1.0-beta", "1.1.0-RC1", "1.0.1")
            val includePrerelease = true

            val stableVersions = allVersions.filterNot { VersionComparator.isPrerelease(it) }
            val target = if (includePrerelease) {
                VersionComparator.selectHighest(allVersions)
            } else {
                VersionComparator.selectHighest(stableVersions)
            }

            // 1.1.0 > 1.0.1, and among 1.1.0 prereleases, RC1 > beta lexicographically
            assertThat(target).isNotNull
            assertThat(VersionComparator.parse(target!!)!!.minor).isEqualTo(1)
        }
    }

    @Nested
    inner class `No updates available` {

        @Test
        fun `empty version list yields no update`() {
            val allVersions = emptyList<String>()
            val latestStable = VersionComparator.selectHighest(allVersions)

            assertThat(latestStable).isNull()
        }

        @Test
        fun `all versions equal to current yield no update`() {
            val currentVersion = "1.0.0"
            val allVersions = listOf("1.0.0")

            val current = VersionComparator.parse(currentVersion)!!
            val latest = VersionComparator.selectHighest(allVersions)
            val target = VersionComparator.parse(latest!!)!!

            assertThat(target <= current).isTrue()
        }

        @Test
        fun `all versions older than current yield no update`() {
            val currentVersion = "3.0.0"
            val allVersions = listOf("1.0.0", "2.0.0", "2.5.0")

            val current = VersionComparator.parse(currentVersion)!!
            val latest = VersionComparator.selectHighest(allVersions)
            val target = VersionComparator.parse(latest!!)!!

            assertThat(target <= current).isTrue()
        }
    }

    @Nested
    inner class `Unparseable current version` {

        @Test
        fun `non-semver current version returns null from parse`() {
            val currentVersion = "latest-release"

            val parsed = VersionComparator.parse(currentVersion)

            assertThat(parsed).isNull()
        }
    }

    @Nested
    inner class `UpdateAvailableInfo construction` {

        @Test
        fun `update info captures all relevant fields`() {
            val current = VersionComparator.parse("1.0.0")!!
            val latest = VersionComparator.parse("1.1.0")!!
            val updateType = VersionComparator.classifyUpdate(current, latest)

            val info = UpdateAvailableInfo(
                alias = "kotlin-stdlib",
                group = "org.jetbrains.kotlin",
                artifact = "kotlin-stdlib",
                currentVersion = "1.0.0",
                latestVersion = "1.1.0",
                latestStable = "1.1.0",
                latestAny = "1.2.0-beta",
                updateType = updateType,
                repository = "Maven Central",
            )

            assertThat(info.alias).isEqualTo("kotlin-stdlib")
            assertThat(info.updateType).isEqualTo(UpdateType.MINOR)
            assertThat(info.currentVersion).isEqualTo("1.0.0")
            assertThat(info.latestVersion).isEqualTo("1.1.0")
            assertThat(info.repository).isEqualTo("Maven Central")
        }
    }

    @Nested
    inner class `MetadataFetchResult variants` {

        @Test
        fun `Success contains versions and repository name`() {
            val result = MetadataFetchResult.Success(
                versions = listOf("1.0.0", "1.1.0", "2.0.0"),
                repository = "Maven Central",
            )

            assertThat(result.versions).hasSize(3)
            assertThat(result.repository).isEqualTo("Maven Central")
        }

        @Test
        fun `NotFound is a singleton`() {
            val result: MetadataFetchResult = MetadataFetchResult.NotFound

            assertThat(result).isInstanceOf(MetadataFetchResult.NotFound::class.java)
        }

        @Test
        fun `Failed captures error message`() {
            val result = MetadataFetchResult.Failed("Connection refused")

            assertThat(result.error).isEqualTo("Connection refused")
        }
    }

    @Nested
    inner class `VersionFetchResult construction` {

        @Test
        fun `contains versions and optional repository`() {
            val result = VersionFetchResult(
                versions = listOf("1.0.0", "2.0.0"),
                repository = "JCenter",
            )

            assertThat(result.versions).containsExactly("1.0.0", "2.0.0")
            assertThat(result.repository).isEqualTo("JCenter")
        }

        @Test
        fun `repository can be null`() {
            val result = VersionFetchResult(
                versions = listOf("1.0.0"),
                repository = null,
            )

            assertThat(result.repository).isNull()
        }
    }
}
