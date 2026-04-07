package io.github.zenhelix.dependanger.metadata

import io.github.zenhelix.dependanger.core.DependangerJson
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.core.spi.SerializationFormat
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

public class JsonSerializationFormat : SerializationFormat<String> {
    override val formatId: String = "json"
    override val fileExtension: String = ".json"
    override val description: String = "JSON format via kotlinx-serialization"

    private val json = DependangerJson

    override fun serialize(metadata: DependangerMetadata): String =
        json.encodeToString(DependangerMetadata.serializer(), metadata)

    override fun deserialize(input: String): DependangerMetadata {
        require(input.isNotBlank()) {
            "Input JSON string must not be blank"
        }

        return try {
            json.decodeFromString(DependangerMetadata.serializer(), input)
        } catch (e: SerializationException) {
            throw MetadataDeserializationException("Failed to deserialize DependangerMetadata from JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw MetadataDeserializationException("Invalid JSON structure for DependangerMetadata: ${e.message}", e)
        }
    }

    override fun write(metadata: DependangerMetadata, path: Path) {
        val jsonString = serialize(metadata)

        try {
            path.parent?.let { parent ->
                if (!parent.exists()) {
                    parent.createDirectories()
                }
            }

            path.writeText(jsonString, Charsets.UTF_8)
        } catch (e: IOException) {
            throw MetadataWriteException("Failed to write metadata to '$path': ${e.message}", e)
        } catch (e: SecurityException) {
            throw MetadataWriteException("Permission denied writing metadata to '$path': ${e.message}", e)
        }
    }

    override fun read(path: Path): DependangerMetadata {
        if (!path.exists()) {
            throw MetadataReadException("Metadata file not found: '$path'", null)
        }

        val content = try {
            path.readText(Charsets.UTF_8)
        } catch (e: IOException) {
            throw MetadataReadException("Failed to read metadata file '$path': ${e.message}", e)
        } catch (e: SecurityException) {
            throw MetadataReadException("Permission denied reading metadata file '$path': ${e.message}", e)
        }

        return try {
            deserialize(content)
        } catch (e: MetadataDeserializationException) {
            throw MetadataReadException("Failed to parse metadata file '$path': ${e.message}", e)
        }
    }
}
