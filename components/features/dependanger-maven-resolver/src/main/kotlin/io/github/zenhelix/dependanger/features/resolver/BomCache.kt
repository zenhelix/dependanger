package io.github.zenhelix.dependanger.features.resolver

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val logger = KotlinLogging.logger {}

private val cacheJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
}

public class BomCache(
    public val cacheDirectory: String,
    public val ttlHours: Long = 24,
    public val ttlSnapshotHours: Long = 1,
) {
    public fun get(group: String, artifact: String, version: String): BomContent? {
        val dir = resolveCacheDir(group, artifact, version)
        val contentFile = dir.resolve("bom-content.json")
        val metaFile = dir.resolve("metadata.json")

        if (!contentFile.exists() || !metaFile.exists()) return null

        return try {
            val meta = cacheJson.decodeFromString<CacheMetadata>(metaFile.readText())
            val ttl = if (version.endsWith("-SNAPSHOT")) ttlSnapshotHours else ttlHours
            val ageHours = (Clock.System.now() - meta.fetchedAt).inWholeHours

            if (ageHours > ttl) return null

            cacheJson.decodeFromString<BomContent>(contentFile.readText())
        } catch (e: Exception) {
            logger.warn { "Corrupted cache for $group:$artifact:$version: ${e.message}" }
            null
        }
    }

    public fun getStale(group: String, artifact: String, version: String): BomContent? {
        val dir = resolveCacheDir(group, artifact, version)
        val contentFile = dir.resolve("bom-content.json")
        if (!contentFile.exists()) return null
        return try {
            cacheJson.decodeFromString<BomContent>(contentFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    public fun put(group: String, artifact: String, version: String, content: BomContent) {
        val dir = resolveCacheDir(group, artifact, version)
        dir.mkdirs()

        val contentFile = dir.resolve("bom-content.json")
        val metaFile = dir.resolve("metadata.json")

        // Atomic write via temp file + move
        writeAtomic(contentFile, cacheJson.encodeToString(content))

        val meta = CacheMetadata(
            fetchedAt = Clock.System.now(),
            isSnapshot = version.endsWith("-SNAPSHOT"),
        )
        writeAtomic(metaFile, cacheJson.encodeToString(meta))
    }

    public fun invalidate(group: String, artifact: String, version: String) {
        val dir = resolveCacheDir(group, artifact, version)
        if (dir.exists()) dir.deleteRecursively()
    }

    public fun clear() {
        val dir = File(cacheDirectory)
        if (dir.exists()) dir.deleteRecursively()
    }

    private fun resolveCacheDir(group: String, artifact: String, version: String): File {
        require(!group.contains("..") && !artifact.contains("..") && !version.contains("..")) {
            "Invalid Maven coordinates containing path traversal: $group:$artifact:$version"
        }
        val groupPath = group.replace('.', '/')
        val resolved = File(cacheDirectory).resolve(groupPath).resolve(artifact).resolve(version)
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
