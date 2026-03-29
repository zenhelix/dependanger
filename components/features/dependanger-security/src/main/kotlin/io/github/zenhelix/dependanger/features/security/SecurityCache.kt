package io.github.zenhelix.dependanger.features.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.cache.AbstractFileCache
import io.github.zenhelix.dependanger.cache.CacheResult
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import java.io.File

private val logger = KotlinLogging.logger {}

public class SecurityCache(
    cacheDirectory: String,
    public val ttlHours: Long,
) : AbstractFileCache(cacheDirectory) {

    public fun get(group: String, artifact: String, version: String): CacheResult<List<VulnerabilityInfo>> {
        val key = "$group:$artifact:$version"
        val file = resolveCacheFile(group, artifact, version)

        if (!file.exists()) return CacheResult.Miss

        return try {
            val entry = cacheJson.decodeFromString<SecurityCacheEntry>(file.readText())

            if (isExpired(entry.fetchedAt, ttlHours)) return CacheResult.Miss

            CacheResult.Hit(entry.vulnerabilities)
        } catch (e: Exception) {
            logger.warn { "Corrupted security cache for $key: ${e.message}" }
            file.delete()
            CacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
        }
    }

    public fun getStale(group: String, artifact: String, version: String): List<VulnerabilityInfo>? {
        val file = resolveCacheFile(group, artifact, version)
        if (!file.exists()) return null
        return try {
            val entry = cacheJson.decodeFromString<SecurityCacheEntry>(file.readText())
            entry.vulnerabilities
        } catch (_: Exception) {
            null
        }
    }

    public fun put(group: String, artifact: String, version: String, vulnerabilities: List<VulnerabilityInfo>) {
        val file = resolveCacheFile(group, artifact, version)
        file.parentFile.mkdirs()

        val entry = SecurityCacheEntry(
            vulnerabilities = vulnerabilities,
            fetchedAt = kotlinx.datetime.Clock.System.now(),
        )
        writeAtomic(file, cacheJson.encodeToString(SecurityCacheEntry.serializer(), entry))
    }

    public fun invalidate(group: String, artifact: String, version: String) {
        val file = resolveCacheFile(group, artifact, version)
        if (file.exists()) file.delete()
    }

    private fun resolveCacheFile(group: String, artifact: String, version: String): File {
        validateSegments(group, artifact, version)
        val resolved = File(cacheDirectory).resolve(group).resolve(artifact).resolve("$version.json")
        validateWithinCacheDir(resolved)
        return resolved
    }
}
