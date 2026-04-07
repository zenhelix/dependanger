package io.github.zenhelix.dependanger.core

import kotlinx.serialization.json.Json

/**
 * Shared [Json] instance used for metadata serialization/deserialization across modules.
 *
 * Configuration:
 * - prettyPrint: human-readable output for metadata files
 * - ignoreUnknownKeys: forward-compatible deserialization
 * - encodeDefaults: explicit values in output for clarity
 */
public val DependangerJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}
