package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import io.github.zenhelix.dependanger.metadata.MetadataReadException
import java.nio.file.Path

public class MetadataService {
    private val format: JsonSerializationFormat = JsonSerializationFormat()

    public fun read(path: Path): DependangerMetadata = try {
        format.read(path)
    } catch (e: MetadataReadException) {
        throw CliException.ParseError("Failed to parse metadata from '$path': ${e.message}", e)
    } catch (e: Exception) {
        throw CliException.ParseError("Failed to read metadata from '$path': ${e.message}", e)
    }

    public fun write(metadata: DependangerMetadata, path: Path) {
        try {
            format.write(metadata, path)
        } catch (e: Exception) {
            throw CliException.ProcessingFailed("Failed to write metadata to '$path': ${e.message}", e)
        }
    }

    public fun serialize(metadata: DependangerMetadata): String =
        format.serialize(metadata)

    public fun emptyMetadata(): DependangerMetadata = DependangerMetadata(
        schemaVersion = DependangerMetadata.SCHEMA_VERSION,
        versions = emptyList(),
        libraries = emptyList(),
        plugins = emptyList(),
        bundles = emptyList(),
        bomImports = emptyList(),
        constraints = emptyList(),
        targetPlatforms = emptyList(),
        distributions = emptyList(),
        compatibility = emptyList(),
        settings = Settings.DEFAULT,
        presets = emptyList(),
    )
}
