package io.github.zenhelix.dependanger.cache

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

public abstract class AbstractFileCache(
    public val cacheDirectory: String,
) {
    protected val cacheJson: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    public fun clear() {
        val dir = File(cacheDirectory)
        if (dir.exists()) dir.deleteRecursively()
    }

    protected fun writeAtomic(target: File, content: String) {
        target.parentFile.mkdirs()
        val tempFile = Files.createTempFile(target.parentFile.toPath(), "tmp-", ".tmp")
        try {
            Files.writeString(tempFile, content)
            Files.move(tempFile, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            Files.deleteIfExists(tempFile)
            throw IOException("Failed to write cache file: $target", e)
        }
    }

    protected fun validateSegments(segments: List<String>) {
        for (segment in segments) {
            require(!segment.contains("..")) {
                "Invalid path segment containing path traversal: $segment"
            }
        }
    }

    protected fun validateWithinCacheDir(resolved: File) {
        val canonical = resolved.canonicalFile
        val cacheCanonical = File(cacheDirectory).canonicalFile
        require(canonical.startsWith(cacheCanonical)) {
            "Resolved cache path escapes cache directory: $resolved"
        }
    }

    protected fun isExpired(fetchedAt: Instant, ttlHours: Long): Boolean {
        val ageHours = (Clock.System.now() - fetchedAt).inWholeHours
        return ageHours > ttlHours
    }
}
