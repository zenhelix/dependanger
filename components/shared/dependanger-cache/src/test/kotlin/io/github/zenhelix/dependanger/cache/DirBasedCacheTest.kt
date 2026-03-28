package io.github.zenhelix.dependanger.cache

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DirBasedCacheTest {

    companion object {
        private const val TTL_HOURS = 24L
        private const val TTL_SNAPSHOT_HOURS = 1L
        private const val CONTENT_FILE_NAME = "content.json"

        private const val GROUP = "io.github.zenhelix"
        private const val ARTIFACT = "some-lib"
        private const val VERSION = "1.0.0"
        private const val SNAPSHOT_VERSION = "1.0.0-SNAPSHOT"
    }

    private fun createCache(tempDir: File): DirBasedCache<String> {
        val cacheDir = tempDir.resolve("cache")
        return DirBasedCache(
            cacheDirectory = cacheDir.absolutePath,
            ttlHours = TTL_HOURS,
            ttlSnapshotHours = TTL_SNAPSHOT_HOURS,
            contentSerializer = String.serializer(),
            contentFileName = CONTENT_FILE_NAME,
        )
    }

    private fun resolveCacheEntryDir(tempDir: File, group: String, artifact: String, version: String): File {
        val groupPath = group.replace('.', '/')
        return tempDir.resolve("cache").resolve(groupPath).resolve(artifact).resolve(version)
    }

    private fun writeExpiredMetadata(dir: File) {
        dir.mkdirs()
        val oldTime = kotlinx.datetime.Instant.fromEpochMilliseconds(0)
        val meta = CacheMetadata(fetchedAt = oldTime, isSnapshot = false)
        val json = Json.encodeToString(CacheMetadata.serializer(), meta)
        dir.resolve("metadata.json").writeText(json)
    }

    private fun writeValidMetadata(dir: File, isSnapshot: Boolean = false) {
        dir.mkdirs()
        val meta = CacheMetadata(fetchedAt = kotlinx.datetime.Clock.System.now(), isSnapshot = isSnapshot)
        val json = Json.encodeToString(CacheMetadata.serializer(), meta)
        dir.resolve("metadata.json").writeText(json)
    }

    private fun writeContent(dir: File, content: String) {
        dir.mkdirs()
        val json = Json.encodeToString(String.serializer(), content)
        dir.resolve(CONTENT_FILE_NAME).writeText(json)
    }

    @Nested
    inner class PutAndGet {

        @Test
        fun `put then get returns Hit with correct data`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)
            val content = "hello world"

            cache.put(GROUP, ARTIFACT, VERSION, content)
            val result = cache.get(GROUP, ARTIFACT, VERSION)

            assertThat(result).isInstanceOf(CacheResult.Hit::class.java)
            assertThat((result as CacheResult.Hit).data).isEqualTo(content)
        }

        @Test
        fun `get from empty cache returns Miss`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)

            val result = cache.get(GROUP, ARTIFACT, VERSION)

            assertThat(result).isEqualTo(CacheResult.Miss)
        }

        @Test
        fun `put overwrites existing entry`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)

            cache.put(GROUP, ARTIFACT, VERSION, "first")
            cache.put(GROUP, ARTIFACT, VERSION, "second")
            val result = cache.get(GROUP, ARTIFACT, VERSION)

            assertThat(result).isInstanceOf(CacheResult.Hit::class.java)
            assertThat((result as CacheResult.Hit).data).isEqualTo("second")
        }
    }

    @Nested
    inner class Expiry {

        @Test
        fun `expired entry returns Miss`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)
            val dir = resolveCacheEntryDir(tempDir, GROUP, ARTIFACT, VERSION)

            writeContent(dir, "cached-value")
            writeExpiredMetadata(dir)

            val result = cache.get(GROUP, ARTIFACT, VERSION)

            assertThat(result).isEqualTo(CacheResult.Miss)
        }

        @Test
        fun `non-expired entry returns Hit`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)
            val content = "fresh-value"

            cache.put(GROUP, ARTIFACT, VERSION, content)
            val result = cache.get(GROUP, ARTIFACT, VERSION)

            assertThat(result).isInstanceOf(CacheResult.Hit::class.java)
            assertThat((result as CacheResult.Hit).data).isEqualTo(content)
        }

        @Test
        fun `SNAPSHOT uses shorter TTL`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)
            val dir = resolveCacheEntryDir(tempDir, GROUP, ARTIFACT, SNAPSHOT_VERSION)

            writeContent(dir, "snapshot-value")
            // Write metadata with fetchedAt 2 hours ago - expired for snapshot (1h TTL) but not for release (24h TTL)
            val twoHoursAgo = kotlinx.datetime.Clock.System.now().minus(kotlin.time.Duration.parse("2h"))
            dir.mkdirs()
            val meta = CacheMetadata(fetchedAt = twoHoursAgo, isSnapshot = true)
            val json = Json.encodeToString(CacheMetadata.serializer(), meta)
            dir.resolve("metadata.json").writeText(json)

            val result = cache.get(GROUP, ARTIFACT, SNAPSHOT_VERSION)

            assertThat(result).isEqualTo(CacheResult.Miss)
        }
    }

    @Nested
    inner class GetStale {

        @Test
        fun `getStale returns content even when expired`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)
            val dir = resolveCacheEntryDir(tempDir, GROUP, ARTIFACT, VERSION)

            writeContent(dir, "stale-value")
            writeExpiredMetadata(dir)

            val result = cache.getStale(GROUP, ARTIFACT, VERSION)

            assertThat(result).isEqualTo("stale-value")
        }

        @Test
        fun `getStale returns null when no entry exists`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)

            val result = cache.getStale(GROUP, ARTIFACT, VERSION)

            assertThat(result).isNull()
        }

        @Test
        fun `getStale returns null on corrupted content`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)
            val dir = resolveCacheEntryDir(tempDir, GROUP, ARTIFACT, VERSION)
            dir.mkdirs()
            dir.resolve(CONTENT_FILE_NAME).writeText("not valid json {{{")

            val result = cache.getStale(GROUP, ARTIFACT, VERSION)

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class Invalidate {

        @Test
        fun `invalidate removes entry, subsequent get returns Miss`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)

            cache.put(GROUP, ARTIFACT, VERSION, "to-be-removed")
            cache.invalidate(GROUP, ARTIFACT, VERSION)
            val result = cache.get(GROUP, ARTIFACT, VERSION)

            assertThat(result).isEqualTo(CacheResult.Miss)
        }

        @Test
        fun `invalidate on non-existent entry does not throw`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)

            cache.invalidate(GROUP, ARTIFACT, VERSION)


        }
    }

    @Nested
    inner class PathSecurity {

        @Test
        fun `path traversal in group segment throws`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)

            assertThatThrownBy { cache.get("../etc", ARTIFACT, VERSION) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("path traversal")
        }

        @Test
        fun `path traversal in artifact segment throws`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)

            assertThatThrownBy { cache.get(GROUP, "../secret", VERSION) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("path traversal")
        }

        @Test
        fun `path traversal in version segment throws`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)

            assertThatThrownBy { cache.get(GROUP, ARTIFACT, "..") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("path traversal")
        }
    }

    @Nested
    inner class CorruptedEntries {

        @Test
        fun `corrupted metadata json returns Corrupted and deletes directory`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)
            val dir = resolveCacheEntryDir(tempDir, GROUP, ARTIFACT, VERSION)
            dir.mkdirs()
            dir.resolve(CONTENT_FILE_NAME).writeText(Json.encodeToString(String.serializer(), "valid"))
            dir.resolve("metadata.json").writeText("not-json{{")

            val result = cache.get(GROUP, ARTIFACT, VERSION)

            assertThat(result).isInstanceOf(CacheResult.Corrupted::class.java)
            assertThat(dir.exists()).isFalse()
        }

        @Test
        fun `corrupted content json returns Corrupted and deletes directory`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)
            val dir = resolveCacheEntryDir(tempDir, GROUP, ARTIFACT, VERSION)
            dir.mkdirs()
            writeValidMetadata(dir)
            dir.resolve(CONTENT_FILE_NAME).writeText("corrupted{{{content")

            val result = cache.get(GROUP, ARTIFACT, VERSION)

            assertThat(result).isInstanceOf(CacheResult.Corrupted::class.java)
            assertThat(dir.exists()).isFalse()
        }
    }

    @Nested
    inner class Clear {

        @Test
        fun `clear removes entire cache directory`(@TempDir tempDir: File) {
            val cache = createCache(tempDir)

            cache.put(GROUP, ARTIFACT, VERSION, "some-data")
            val cacheDir = tempDir.resolve("cache")
            assertThat(cacheDir.exists()).isTrue()

            cache.clear()

            assertThat(cacheDir.exists()).isFalse()
        }

        @Test
        fun `clear on non-existent directory does not throw`(@TempDir tempDir: File) {
            val cacheDir = tempDir.resolve("non-existent-cache")
            val cache = DirBasedCache(
                cacheDirectory = cacheDir.absolutePath,
                ttlHours = TTL_HOURS,
                ttlSnapshotHours = TTL_SNAPSHOT_HOURS,
                contentSerializer = String.serializer(),
                contentFileName = CONTENT_FILE_NAME,
            )

            cache.clear()


        }
    }
}
