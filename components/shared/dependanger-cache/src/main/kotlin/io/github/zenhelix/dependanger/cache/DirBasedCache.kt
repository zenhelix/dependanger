package io.github.zenhelix.dependanger.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import java.io.File

private val logger = KotlinLogging.logger {}

public class DirBasedCache<T>(
    cacheDirectory: String,
    public val ttlHours: Long,
    public val ttlSnapshotHours: Long,
    private val contentSerializer: KSerializer<T>,
    private val contentFileName: String,
) : AbstractFileCache(cacheDirectory) {

    public fun get(group: String, artifact: String, version: String): CacheResult<T> {
        val key = "$group:$artifact:$version"
        val dir = resolveCacheDir(group, artifact, version)
        val contentFile = dir.resolve(contentFileName)
        val metaFile = dir.resolve("metadata.json")

        if (!contentFile.exists() || !metaFile.exists()) return CacheResult.Miss

        return try {
            val meta = cacheJson.decodeFromString(CacheMetadata.serializer(), metaFile.readText())
            val ttl = if (version.endsWith("-SNAPSHOT")) ttlSnapshotHours else ttlHours

            if (isExpired(meta.fetchedAt, ttl)) return CacheResult.Miss

            CacheResult.Hit(cacheJson.decodeFromString(contentSerializer, contentFile.readText()))
        } catch (e: Exception) {
            logger.warn { "Corrupted cache for $key: ${e.message}" }
            dir.deleteRecursively()
            CacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
        }
    }

    public fun getStale(group: String, artifact: String, version: String): T? {
        val dir = resolveCacheDir(group, artifact, version)
        val contentFile = dir.resolve(contentFileName)
        if (!contentFile.exists()) return null
        return try {
            cacheJson.decodeFromString(contentSerializer, contentFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    public fun put(group: String, artifact: String, version: String, content: T) {
        val dir = resolveCacheDir(group, artifact, version)
        dir.mkdirs()

        val contentFile = dir.resolve(contentFileName)
        val metaFile = dir.resolve("metadata.json")

        writeAtomic(contentFile, cacheJson.encodeToString(contentSerializer, content))

        val meta = CacheMetadata(
            fetchedAt = Clock.System.now(),
            isSnapshot = version.endsWith("-SNAPSHOT"),
        )
        writeAtomic(metaFile, cacheJson.encodeToString(CacheMetadata.serializer(), meta))
    }

    public fun invalidate(group: String, artifact: String, version: String) {
        val dir = resolveCacheDir(group, artifact, version)
        if (dir.exists()) dir.deleteRecursively()
    }

    private fun resolveCacheDir(group: String, artifact: String, version: String): File {
        validateSegments(group, artifact, version)
        val groupPath = group.replace('.', '/')
        val resolved = File(cacheDirectory).resolve(groupPath).resolve(artifact).resolve(version)
        validateWithinCacheDir(resolved)
        return resolved
    }
}
