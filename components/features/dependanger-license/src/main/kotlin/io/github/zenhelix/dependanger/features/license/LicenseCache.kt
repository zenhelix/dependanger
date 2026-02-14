package io.github.zenhelix.dependanger.features.license

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.features.license.model.LicenseResult
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val logger = KotlinLogging.logger {}

public sealed interface LicenseCacheResult {
    public data class Hit(val licenses: List<LicenseResult>) : LicenseCacheResult
    public data object Miss : LicenseCacheResult
    public data class Corrupted(val key: String, val error: String) : LicenseCacheResult
}

@Serializable
internal data class LicenseCacheMetadata(
    val fetchedAt: Instant,
    val isSnapshot: Boolean,
)

public class LicenseCache(
    public val cacheDirectory: String,
    public val ttlHours: Long,
    public val ttlSnapshotHours: Long,
) {
    private val cacheJson: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    public fun get(group: String, artifact: String, version: String): LicenseCacheResult {
        val key = "$group:$artifact:$version"
        val dir = resolveCacheDir(group, artifact, version)
        val contentFile = dir.resolve("license-content.json")
        val metaFile = dir.resolve("metadata.json")

        if (!contentFile.exists() || !metaFile.exists()) return LicenseCacheResult.Miss

        return try {
            val meta = cacheJson.decodeFromString<LicenseCacheMetadata>(metaFile.readText())
            val ttl = if (version.endsWith("-SNAPSHOT")) ttlSnapshotHours else ttlHours
            val ageHours = (Clock.System.now() - meta.fetchedAt).inWholeHours

            if (ageHours > ttl) return LicenseCacheResult.Miss

            LicenseCacheResult.Hit(cacheJson.decodeFromString<List<LicenseResult>>(contentFile.readText()))
        } catch (e: Exception) {
            logger.warn { "Corrupted cache for $key: ${e.message}" }
            dir.deleteRecursively()
            LicenseCacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
        }
    }

    public fun getStale(group: String, artifact: String, version: String): List<LicenseResult>? {
        val dir = resolveCacheDir(group, artifact, version)
        val contentFile = dir.resolve("license-content.json")
        if (!contentFile.exists()) return null
        return try {
            cacheJson.decodeFromString<List<LicenseResult>>(contentFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    public fun put(group: String, artifact: String, version: String, licenses: List<LicenseResult>) {
        val dir = resolveCacheDir(group, artifact, version)
        dir.mkdirs()

        val contentFile = dir.resolve("license-content.json")
        val metaFile = dir.resolve("metadata.json")

        writeAtomic(contentFile, cacheJson.encodeToString(licenses))

        val meta = LicenseCacheMetadata(
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
