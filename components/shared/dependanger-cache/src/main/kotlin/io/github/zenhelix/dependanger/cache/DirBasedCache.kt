package io.github.zenhelix.dependanger.cache

import kotlinx.serialization.KSerializer
import java.io.File

public class DirBasedCache<T>(
    cacheDirectory: String,
    public val ttlHours: Long,
    public val ttlSnapshotHours: Long,
    contentSerializer: KSerializer<T>,
    private val keyResolver: CacheKeyResolver = CacheKeyResolver.MavenGroupArtifactVersion,
    contentFileName: String = "content.json",
) : AbstractFileCache(cacheDirectory) {

    private val layout: CacheLayout<T> = if (keyResolver.usesDirectoryLayout) {
        DirectoryCacheLayout(cacheJson, contentSerializer, contentFileName, ::writeAtomic)
    } else {
        SingleFileCacheLayout(cacheJson, contentSerializer, ::writeAtomic)
    }

    public fun get(segments: List<String>): CacheResult<T> {
        val key = keyResolver.keyString(segments)
        val location = resolveLocation(segments)
        return layout.read(location, key) { fetchedAt -> isExpired(fetchedAt, selectTtl(segments)) }
    }

    public fun get(group: String, artifact: String, version: String): CacheResult<T> =
        get(listOf(group, artifact, version))

    public fun getStale(segments: List<String>): T? {
        val location = resolveLocation(segments)
        return layout.readStale(location)
    }

    public fun getStale(group: String, artifact: String, version: String): T? =
        getStale(listOf(group, artifact, version))

    public fun put(content: T, segments: List<String>) {
        val location = resolveLocation(segments)
        layout.write(location, content, isSnapshot(segments))
    }

    public fun put(group: String, artifact: String, version: String, content: T) {
        put(content, listOf(group, artifact, version))
    }

    public fun invalidate(segments: List<String>) {
        val location = resolveLocation(segments)
        layout.delete(location)
    }

    public fun invalidate(group: String, artifact: String, version: String) {
        invalidate(listOf(group, artifact, version))
    }

    private fun resolveLocation(segments: List<String>): File {
        validateSegments(segments)
        val resolved = keyResolver.resolve(File(cacheDirectory), segments)
        validateWithinCacheDir(resolved)
        return resolved
    }

    private fun isSnapshot(segments: List<String>): Boolean {
        if (keyResolver.segmentCount < 3) return false
        return segments.getOrNull(2)?.endsWith("-SNAPSHOT") == true
    }

    private fun selectTtl(segments: List<String>): Long =
        if (isSnapshot(segments)) ttlSnapshotHours else ttlHours
}
