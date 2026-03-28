package io.github.zenhelix.dependanger.effective.serialization

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.spi.ExtensionSerializerProvider
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.nio.file.Path
import java.util.ServiceLoader
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger {}

public class EffectiveJsonFormat {

    public val formatId: String = "effective-json"
    public val fileExtension: String = ".effective.json"

    private val providers: List<ExtensionSerializerProvider> =
        ServiceLoader.load(ExtensionSerializerProvider::class.java).toList()

    private val knownKeys: Map<String, ExtensionKey<*>> =
        providers.flatMap { it.knownKeys().entries }.associate { it.key to it.value }

    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    public fun serialize(metadata: EffectiveMetadata): String {
        val baseJson = json.encodeToJsonElement(EffectiveMetadata.serializer(), metadata).jsonObject

        val resultJson = if (metadata.extensions.isNotEmpty()) {
            val extensionsJson = buildJsonObject {
                for ((key, value) in metadata.extensions) {
                    @Suppress("UNCHECKED_CAST")
                    val serializer = key.serializer as KSerializer<Any>
                    put(key.name, json.encodeToJsonElement(serializer, value))
                }
            }
            JsonObject(baseJson + ("extensions" to extensionsJson))
        } else {
            baseJson
        }

        return json.encodeToString(JsonElement.serializer(), resultJson)
    }

    public fun deserialize(input: String): EffectiveMetadata {
        require(input.isNotBlank()) { "Input JSON string must not be blank" }

        val jsonObject = json.parseToJsonElement(input).jsonObject
        val base = json.decodeFromJsonElement(EffectiveMetadata.serializer(), jsonObject)

        val extensionsJson = jsonObject["extensions"]?.jsonObject ?: return base

        val extensions = buildMap<ExtensionKey<*>, Any> {
            for ((keyName, element) in extensionsJson) {
                val extensionKey = knownKeys[keyName]
                if (extensionKey == null) {
                    logger.warn { "Unknown extension key '$keyName', skipping" }
                    continue
                }
                try {
                    val deserialized = json.decodeFromJsonElement(extensionKey.serializer, element)
                    put(extensionKey, deserialized)
                } catch (e: Exception) {
                    logger.warn { "Failed to deserialize extension '$keyName': ${e.message}" }
                }
            }
        }

        return base.copy(extensions = extensions)
    }

    public fun write(metadata: EffectiveMetadata, path: Path) {
        val jsonString = serialize(metadata)
        path.parent?.let { parent ->
            if (!parent.exists()) {
                parent.createDirectories()
            }
        }
        path.writeText(jsonString, Charsets.UTF_8)
    }

    public fun read(path: Path): EffectiveMetadata {
        if (!path.exists()) {
            throw IllegalArgumentException("Effective metadata file not found: '$path'")
        }
        val content = path.readText(Charsets.UTF_8)
        return deserialize(content)
    }
}
