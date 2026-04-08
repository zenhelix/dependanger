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
    private val keyResolver: CacheKeyResolver = CacheKeyResolver.MavenGroupArtifactVersion,
    private val contentFileName: String = "content.json",
) : AbstractFileCache(cacheDirectory) {

    public fun get(segments: List<String>): CacheResult<T> {
        val key = keyResolver.keyString(segments)
        return if (keyResolver.usesDirectoryLayout) getFromDir(segments, key) else getFromFile(segments, key)
    }

    public fun get(group: String, artifact: String, version: String): CacheResult<T> =
        get(listOf(group, artifact, version))

    public fun getStale(segments: List<String>): T? {
        return if (keyResolver.usesDirectoryLayout) getStaleFromDir(segments) else getStaleFromFile(segments)
    }

    public fun getStale(group: String, artifact: String, version: String): T? =
        getStale(listOf(group, artifact, version))

    public fun put(content: T, segments: List<String>) {
        if (keyResolver.usesDirectoryLayout) putToDir(content, segments) else putToFile(content, segments)
    }

    public fun put(group: String, artifact: String, version: String, content: T) {
        put(content, listOf(group, artifact, version))
    }

    public fun invalidate(segments: List<String>) {
        val resolved = resolveLocation(segments)
        if (keyResolver.usesDirectoryLayout) {
            if (resolved.exists()) resolved.deleteRecursively()
        } else {
            if (resolved.exists()) resolved.delete()
        }
    }

    public fun invalidate(group: String, artifact: String, version: String) {
        invalidate(listOf(group, artifact, version))
    }

    // --- Directory layout (content file + metadata.json in a subdirectory) ---

    private fun getFromDir(segments: List<String>, key: String): CacheResult<T> {
        val dir = resolveLocation(segments)
        val contentFile = dir.resolve(contentFileName)
        val metaFile = dir.resolve("metadata.json")

        if (!contentFile.exists() || !metaFile.exists()) return CacheResult.Miss

        return try {
            val meta = cacheJson.decodeFromString(CacheMetadata.serializer(), metaFile.readText())
            val ttl = selectTtl(segments)

            if (isExpired(meta.fetchedAt, ttl)) return CacheResult.Miss

            CacheResult.Hit(cacheJson.decodeFromString(contentSerializer, contentFile.readText()))
        } catch (e: Exception) {
            logger.warn { "Corrupted cache for $key: ${e.message}" }
            dir.deleteRecursively()
            CacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
        }
    }

    private fun getStaleFromDir(segments: List<String>): T? {
        val dir = resolveLocation(segments)
        val contentFile = dir.resolve(contentFileName)
        if (!contentFile.exists()) return null
        return try {
            cacheJson.decodeFromString(contentSerializer, contentFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    private fun putToDir(content: T, segments: List<String>) {
        val dir = resolveLocation(segments)
        dir.mkdirs()

        val contentFile = dir.resolve(contentFileName)
        val metaFile = dir.resolve("metadata.json")

        writeAtomic(contentFile, cacheJson.encodeToString(contentSerializer, content))

        val version = segments.getOrNull(2)
        val meta = CacheMetadata(
            fetchedAt = Clock.System.now(),
            isSnapshot = version?.endsWith("-SNAPSHOT") == true,
        )
        writeAtomic(metaFile, cacheJson.encodeToString(CacheMetadata.serializer(), meta))
    }

    // --- Single file layout (one JSON file with embedded fetchedAt) ---

    private fun getFromFile(segments: List<String>, key: String): CacheResult<T> {
        val file = resolveLocation(segments)
        if (!file.exists()) return CacheResult.Miss

        return try {
            val wrapper = cacheJson.decodeFromString(CacheEntryWrapper.serializer(contentSerializer), file.readText())

            if (isExpired(wrapper.fetchedAt, selectTtl(segments))) return CacheResult.Miss

            CacheResult.Hit(wrapper.content)
        } catch (e: Exception) {
            logger.warn { "Corrupted cache for $key: ${e.message}" }
            file.delete()
            CacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
        }
    }

    private fun getStaleFromFile(segments: List<String>): T? {
        val file = resolveLocation(segments)
        if (!file.exists()) return null
        return try {
            cacheJson.decodeFromString(CacheEntryWrapper.serializer(contentSerializer), file.readText()).content
        } catch (_: Exception) {
            null
        }
    }

    private fun putToFile(content: T, segments: List<String>) {
        val file = resolveLocation(segments)
        file.parentFile.mkdirs()

        val wrapper = CacheEntryWrapper(
            content = content,
            fetchedAt = Clock.System.now(),
        )
        writeAtomic(file, cacheJson.encodeToString(CacheEntryWrapper.serializer(contentSerializer), wrapper))
    }

    // --- Shared helpers ---

    private fun resolveLocation(segments: List<String>): File {
        validateSegments(segments)
        val resolved = keyResolver.resolve(File(cacheDirectory), segments)
        validateWithinCacheDir(resolved)
        return resolved
    }

    private fun selectTtl(segments: List<String>): Long {
        val version = if (keyResolver.segmentCount >= 3) segments.getOrNull(2) else null
        return if (version?.endsWith("-SNAPSHOT") == true) ttlSnapshotHours else ttlHours
    }
}
