package io.github.zenhelix.dependanger.features.updates

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.cache.AbstractFileCache
import io.github.zenhelix.dependanger.cache.CacheResult
import java.io.File

private val logger = KotlinLogging.logger {}

public class VersionCache(
    cacheDirectory: String,
    public val ttlHours: Long,
) : AbstractFileCache(cacheDirectory) {

    public fun get(group: String, artifact: String): CacheResult<VersionFetchResult> {
        val key = "$group:$artifact"
        val file = resolveCacheFile(group, artifact)

        if (!file.exists()) return CacheResult.Miss

        return try {
            val entry = cacheJson.decodeFromString<VersionCacheEntry>(file.readText())

            if (isExpired(entry.fetchedAt, ttlHours)) return CacheResult.Miss

            CacheResult.Hit(
                VersionFetchResult(
                    versions = entry.versions,
                    repository = entry.repository,
                )
            )
        } catch (e: Exception) {
            logger.warn { "Corrupted version cache for $key: ${e.message}" }
            file.delete()
            CacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
        }
    }

    public fun getStale(group: String, artifact: String): VersionFetchResult? {
        val file = resolveCacheFile(group, artifact)
        if (!file.exists()) return null
        return try {
            val entry = cacheJson.decodeFromString<VersionCacheEntry>(file.readText())
            VersionFetchResult(
                versions = entry.versions,
                repository = entry.repository,
            )
        } catch (_: Exception) {
            null
        }
    }

    public fun put(group: String, artifact: String, result: VersionFetchResult) {
        val file = resolveCacheFile(group, artifact)

        val entry = VersionCacheEntry(
            versions = result.versions,
            fetchedAt = kotlinx.datetime.Clock.System.now(),
            repository = result.repository,
        )
        writeAtomic(file, cacheJson.encodeToString(VersionCacheEntry.serializer(), entry))
    }

    public fun invalidate(group: String, artifact: String) {
        val file = resolveCacheFile(group, artifact)
        if (file.exists()) file.delete()
    }

    private fun resolveCacheFile(group: String, artifact: String): File {
        validateSegments(group, artifact)
        val resolved = File(cacheDirectory).resolve(group).resolve("$artifact.json")
        validateWithinCacheDir(resolved)
        return resolved
    }
}
