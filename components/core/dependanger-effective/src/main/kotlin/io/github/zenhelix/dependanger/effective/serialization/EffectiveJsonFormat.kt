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

public class EffectiveJsonFormat : EffectiveSerializationFormat<String> {

    public override val formatId: String = "effective-json"
    public override val fileExtension: String = ".effective.json"

    private val providers: List<ExtensionSerializerProvider> =
        ServiceLoader.load(ExtensionSerializerProvider::class.java).toList()

    private val knownKeys: Map<String, ExtensionKey<*>> =
        providers.flatMap { it.knownKeys().entries }.associate { it.key to it.value }

    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    public override fun serialize(metadata: EffectiveMetadata): String {
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

    public override fun deserialize(input: String): EffectiveMetadata = deserializeDetailed(input).metadata

    public override fun deserializeDetailed(input: String): DeserializationResult {
        require(input.isNotBlank()) { "Input JSON string must not be blank" }

        val jsonObject = json.parseToJsonElement(input).jsonObject
        val base = json.decodeFromJsonElement(EffectiveMetadata.serializer(), jsonObject)

        val extensionsJson = jsonObject["extensions"]?.jsonObject
            ?: return DeserializationResult(base, emptyList())

        val extensions = mutableMapOf<ExtensionKey<*>, Any>()
        val warnings = mutableListOf<DeserializationWarning>()

        for ((keyName, element) in extensionsJson) {
            val extensionKey = knownKeys[keyName]
            if (extensionKey == null) {
                warnings.add(DeserializationWarning(keyName, "Unknown extension key '$keyName', skipping"))
                logger.warn { "Unknown extension key '$keyName', skipping" }
                continue
            }
            try {
                val deserialized = json.decodeFromJsonElement(extensionKey.serializer, element)
                extensions[extensionKey] = deserialized
            } catch (e: Exception) {
                warnings.add(DeserializationWarning(keyName, "Failed to deserialize extension '$keyName': ${e.message}", e))
                logger.warn { "Failed to deserialize extension '$keyName': ${e.message}" }
            }
        }

        return DeserializationResult(base.copy(extensions = extensions), warnings)
    }

    public override fun write(metadata: EffectiveMetadata, path: Path) {
        val jsonString = serialize(metadata)
        path.parent?.let { parent ->
            if (!parent.exists()) {
                parent.createDirectories()
            }
        }
        path.writeText(jsonString, Charsets.UTF_8)
    }

    public override fun read(path: Path): EffectiveMetadata = readDetailed(path).metadata

    public override fun readDetailed(path: Path): DeserializationResult {
        if (!path.exists()) {
            throw IllegalArgumentException("Effective metadata file not found: '$path'")
        }
        val content = path.readText(Charsets.UTF_8)
        return deserializeDetailed(content)
    }
}
