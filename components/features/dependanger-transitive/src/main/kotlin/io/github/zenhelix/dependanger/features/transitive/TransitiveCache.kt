package io.github.zenhelix.dependanger.features.transitive

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.features.transitive.model.TransitiveTree
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val logger = KotlinLogging.logger {}

public sealed interface TransitiveCacheResult {
    public data class Hit(val tree: TransitiveTree) : TransitiveCacheResult
    public data object Miss : TransitiveCacheResult
    public data class Corrupted(val key: String, val error: String) : TransitiveCacheResult
}

@Serializable
internal data class TransitiveCacheMetadata(
    val fetchedAt: Instant,
    val isSnapshot: Boolean,
)

public class TransitiveCache(
    public val cacheDirectory: String,
    public val ttlHours: Long,
    public val ttlSnapshotHours: Long,
) {
    private val cacheJson: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    public fun get(group: String, artifact: String, version: String): TransitiveCacheResult {
        val key = "$group:$artifact:$version"
        val dir = resolveCacheDir(group, artifact, version)
        val contentFile = dir.resolve("tree-content.json")
        val metaFile = dir.resolve("metadata.json")

        if (!contentFile.exists() || !metaFile.exists()) return TransitiveCacheResult.Miss

        return try {
            val meta = cacheJson.decodeFromString<TransitiveCacheMetadata>(metaFile.readText())
            val ttl = if (version.endsWith("-SNAPSHOT")) ttlSnapshotHours else ttlHours
            val ageHours = (Clock.System.now() - meta.fetchedAt).inWholeHours

            if (ageHours > ttl) return TransitiveCacheResult.Miss

            TransitiveCacheResult.Hit(cacheJson.decodeFromString<TransitiveTree>(contentFile.readText()))
        } catch (e: Exception) {
            logger.warn { "Corrupted transitive cache for $key: ${e.message}" }
            dir.deleteRecursively()
            TransitiveCacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
        }
    }

    public fun getStale(group: String, artifact: String, version: String): TransitiveTree? {
        val dir = resolveCacheDir(group, artifact, version)
        val contentFile = dir.resolve("tree-content.json")
        if (!contentFile.exists()) return null
        return try {
            cacheJson.decodeFromString<TransitiveTree>(contentFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    public fun put(group: String, artifact: String, version: String, tree: TransitiveTree) {
        val dir = resolveCacheDir(group, artifact, version)
        dir.mkdirs()

        val contentFile = dir.resolve("tree-content.json")
        val metaFile = dir.resolve("metadata.json")

        writeAtomic(contentFile, cacheJson.encodeToString(tree))

        val meta = TransitiveCacheMetadata(
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
            throw IOException("Failed to write transitive cache file: $target", e)
        }
    }
}
