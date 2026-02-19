package io.github.zenhelix.dependanger.features.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val logger = KotlinLogging.logger {}

public class SecurityCache(
    public val cacheDirectory: String,
    public val ttlHours: Long,
) {
    private val cacheJson: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    public fun get(group: String, artifact: String, version: String): SecurityCacheResult {
        val key = "$group:$artifact:$version"
        val file = resolveCacheFile(group, artifact, version)

        if (!file.exists()) return SecurityCacheResult.Miss

        return try {
            val entry = cacheJson.decodeFromString<SecurityCacheEntry>(file.readText())
            val ageHours = (Clock.System.now() - entry.fetchedAt).inWholeHours

            if (ageHours > ttlHours) return SecurityCacheResult.Miss

            SecurityCacheResult.Hit(entry.vulnerabilities)
        } catch (e: Exception) {
            logger.warn { "Corrupted security cache for $key: ${e.message}" }
            file.delete()
            SecurityCacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
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
            fetchedAt = Clock.System.now(),
        )
        writeAtomic(file, cacheJson.encodeToString(SecurityCacheEntry.serializer(), entry))
    }

    public fun invalidate(group: String, artifact: String, version: String) {
        val file = resolveCacheFile(group, artifact, version)
        if (file.exists()) file.delete()
    }

    public fun clear() {
        val dir = File(cacheDirectory)
        if (dir.exists()) dir.deleteRecursively()
    }

    private fun resolveCacheFile(group: String, artifact: String, version: String): File {
        require(!group.contains("..") && !artifact.contains("..") && !version.contains("..")) {
            "Invalid Maven coordinates containing path traversal: $group:$artifact:$version"
        }
        val resolved = File(cacheDirectory).resolve(group).resolve(artifact).resolve("$version.json")
        val canonical = resolved.canonicalFile
        val cacheCanonical = File(cacheDirectory).canonicalFile
        require(canonical.startsWith(cacheCanonical)) {
            "Resolved cache path escapes cache directory: $resolved"
        }
        return resolved
    }

    private fun writeAtomic(target: File, content: String) {
        val tempFile = Files.createTempFile(target.parentFile.toPath(), "tmp-", ".tmp")
        try {
            Files.writeString(tempFile, content)
            Files.move(tempFile, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: Exception) {
            Files.deleteIfExists(tempFile)
            throw IOException("Failed to write cache file: $target", e)
        }
    }
}

public sealed interface SecurityCacheResult {
    public data class Hit(val vulnerabilities: List<VulnerabilityInfo>) : SecurityCacheResult
    public data object Miss : SecurityCacheResult
    public data class Corrupted(val key: String, val error: String) : SecurityCacheResult
}
