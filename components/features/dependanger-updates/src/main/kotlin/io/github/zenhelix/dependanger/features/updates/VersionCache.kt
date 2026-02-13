package io.github.zenhelix.dependanger.features.updates

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val logger = KotlinLogging.logger {}

public class VersionCache(
    public val cacheDirectory: String,
    public val ttlHours: Long,
) {
    private val cacheJson: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    public fun get(group: String, artifact: String): VersionCacheResult {
        val key = "$group:$artifact"
        val file = resolveCacheFile(group, artifact)

        if (!file.exists()) return VersionCacheResult.Miss

        return try {
            val entry = cacheJson.decodeFromString<VersionCacheEntry>(file.readText())
            val ageHours = (Clock.System.now() - entry.fetchedAt).inWholeHours

            if (ageHours > ttlHours) return VersionCacheResult.Miss

            VersionCacheResult.Hit(
                VersionFetchResult(
                    versions = entry.versions,
                    repository = entry.repository,
                )
            )
        } catch (e: Exception) {
            logger.warn { "Corrupted version cache for $key: ${e.message}" }
            file.delete()
            VersionCacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
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
        file.parentFile.mkdirs()

        val entry = VersionCacheEntry(
            versions = result.versions,
            fetchedAt = Clock.System.now(),
            repository = result.repository,
        )
        writeAtomic(file, cacheJson.encodeToString(VersionCacheEntry.serializer(), entry))
    }

    public fun invalidate(group: String, artifact: String) {
        val file = resolveCacheFile(group, artifact)
        if (file.exists()) file.delete()
    }

    public fun clear() {
        val dir = File(cacheDirectory)
        if (dir.exists()) dir.deleteRecursively()
    }

    private fun resolveCacheFile(group: String, artifact: String): File {
        require(!group.contains("..") && !artifact.contains("..")) {
            "Invalid Maven coordinates containing path traversal: $group:$artifact"
        }
        val resolved = File(cacheDirectory).resolve(group).resolve("$artifact.json")
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
