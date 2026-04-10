package io.github.zenhelix.dependanger.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

/**
 * Strategy for reading/writing cache entries on disk.
 * [DirectoryCacheLayout] stores content and metadata as separate files in a directory.
 * [SingleFileCacheLayout] stores content and metadata in a single JSON file.
 */
internal sealed interface CacheLayout<T> {
    fun read(location: File, key: String, isExpired: (Instant) -> Boolean): CacheResult<T>
    fun readStale(location: File): T?
    fun write(location: File, content: T, isSnapshot: Boolean)
    fun delete(location: File)
}

/**
 * Directory layout: content file + metadata.json as separate files in a subdirectory.
 * Used by BOM cache, License cache, Transitive cache.
 */
internal class DirectoryCacheLayout<T>(
    private val json: Json,
    private val contentSerializer: KSerializer<T>,
    private val contentFileName: String,
    private val writeAtomic: (File, String) -> Unit,
) : CacheLayout<T> {

    override fun read(location: File, key: String, isExpired: (Instant) -> Boolean): CacheResult<T> {
        val contentFile = location.resolve(contentFileName)
        val metaFile = location.resolve(METADATA_FILE_NAME)

        if (!contentFile.exists() || !metaFile.exists()) return CacheResult.Miss

        return try {
            val meta = json.decodeFromString(CacheMetadata.serializer(), metaFile.readText())
            if (isExpired(meta.fetchedAt)) return CacheResult.Miss
            CacheResult.Hit(json.decodeFromString(contentSerializer, contentFile.readText()))
        } catch (e: Exception) {
            logger.warn { "Corrupted cache for $key: ${e.message}" }
            location.deleteRecursively()
            CacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
        }
    }

    override fun readStale(location: File): T? {
        val contentFile = location.resolve(contentFileName)
        if (!contentFile.exists()) return null
        return try {
            json.decodeFromString(contentSerializer, contentFile.readText())
        } catch (_: SerializationException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    override fun write(location: File, content: T, isSnapshot: Boolean) {
        location.mkdirs()
        writeAtomic(location.resolve(contentFileName), json.encodeToString(contentSerializer, content))
        val meta = CacheMetadata(fetchedAt = Clock.System.now(), isSnapshot = isSnapshot)
        writeAtomic(location.resolve(METADATA_FILE_NAME), json.encodeToString(CacheMetadata.serializer(), meta))
    }

    override fun delete(location: File) {
        if (location.exists()) location.deleteRecursively()
    }

    private companion object {
        private const val METADATA_FILE_NAME = "metadata.json"
    }
}

/**
 * Single file layout: content and fetchedAt wrapped in a single JSON file.
 * Used by Security cache, Version/Updates cache.
 */
internal class SingleFileCacheLayout<T>(
    private val json: Json,
    private val contentSerializer: KSerializer<T>,
    private val writeAtomic: (File, String) -> Unit,
) : CacheLayout<T> {

    override fun read(location: File, key: String, isExpired: (Instant) -> Boolean): CacheResult<T> {
        if (!location.exists()) return CacheResult.Miss

        return try {
            val wrapper = json.decodeFromString(CacheEntryWrapper.serializer(contentSerializer), location.readText())
            if (isExpired(wrapper.fetchedAt)) return CacheResult.Miss
            CacheResult.Hit(wrapper.content)
        } catch (e: Exception) {
            logger.warn { "Corrupted cache for $key: ${e.message}" }
            location.delete()
            CacheResult.Corrupted(key = key, error = e.message ?: "Unknown error")
        }
    }

    override fun readStale(location: File): T? {
        if (!location.exists()) return null
        return try {
            json.decodeFromString(CacheEntryWrapper.serializer(contentSerializer), location.readText()).content
        } catch (_: SerializationException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    override fun write(location: File, content: T, @Suppress("UNUSED_PARAMETER") isSnapshot: Boolean) {
        location.parentFile.mkdirs()
        val wrapper = CacheEntryWrapper(content = content, fetchedAt = Clock.System.now())
        writeAtomic(location, json.encodeToString(CacheEntryWrapper.serializer(contentSerializer), wrapper))
    }

    override fun delete(location: File) {
        if (location.exists()) location.delete()
    }
}
