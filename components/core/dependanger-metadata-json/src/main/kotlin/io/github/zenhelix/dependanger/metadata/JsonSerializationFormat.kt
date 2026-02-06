package io.github.zenhelix.dependanger.metadata

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.core.spi.SerializationFormat
import kotlinx.serialization.json.Json
import java.nio.file.Path

public class JsonSerializationFormat : SerializationFormat<String> {
    override val formatId: String = "json"
    override val fileExtension: String = ".json"

    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun serialize(metadata: DependangerMetadata): String = TODO()
    override fun deserialize(input: String): DependangerMetadata = TODO()
    override fun write(metadata: DependangerMetadata, path: Path): Unit = TODO()
    override fun read(path: Path): DependangerMetadata = TODO()
}
