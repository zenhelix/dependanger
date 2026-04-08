package io.github.zenhelix.dependanger.cache

import java.io.File

/**
 * Strategy for resolving cache file/directory paths from key segments.
 */
public sealed interface CacheKeyResolver {

    /**
     * Resolve the cache location from key segments.
     * Returns the File where content will be stored (either a directory or a file).
     */
    public fun resolve(cacheDirectory: File, segments: List<String>): File

    /**
     * Build a human-readable key string for logging/diagnostics.
     */
    public fun keyString(segments: List<String>): String

    /**
     * Whether this resolver uses a directory layout (content + metadata as separate files)
     * or a single-file layout (content file only, no metadata.json).
     */
    public val usesDirectoryLayout: Boolean get() = true

    /**
     * The expected number of key segments.
     */
    public val segmentCount: Int

    /**
     * Maven-style directory layout: group dots->slashes, then artifact, then version as subdirectories.
     * Layout: `<group.as.path>/<artifact>/<version>/` (directory with content + metadata files)
     * Used by: BOM cache, License cache, Transitive cache.
     */
    public data object MavenGroupArtifactVersion : CacheKeyResolver {
        override val segmentCount: Int = 3
        override fun resolve(cacheDirectory: File, segments: List<String>): File {
            require(segments.size == 3) { "MavenGroupArtifactVersion requires 3 segments (group, artifact, version), got ${segments.size}" }
            val (group, artifact, version) = segments
            val groupPath = group.replace('.', '/')
            return cacheDirectory.resolve(groupPath).resolve(artifact).resolve(version)
        }
        override fun keyString(segments: List<String>): String = segments.joinToString(":")
    }

    /**
     * Flat file layout with 3 key segments: group/artifact/version.json
     * Layout: `<group>/<artifact>/<version>.json` (single file, no metadata.json)
     * Used by: Security cache.
     */
    public data object FlatGroupArtifactVersion : CacheKeyResolver {
        override val segmentCount: Int = 3
        override val usesDirectoryLayout: Boolean = false
        override fun resolve(cacheDirectory: File, segments: List<String>): File {
            require(segments.size == 3) { "FlatGroupArtifactVersion requires 3 segments (group, artifact, version), got ${segments.size}" }
            val (group, artifact, version) = segments
            return cacheDirectory.resolve(group).resolve(artifact).resolve("$version.json")
        }
        override fun keyString(segments: List<String>): String = segments.joinToString(":")
    }

    /**
     * Flat file layout with 2 key segments: group/artifact.json
     * Layout: `<group>/<artifact>.json` (single file, no metadata.json)
     * Used by: Version cache.
     */
    public data object FlatGroupArtifact : CacheKeyResolver {
        override val segmentCount: Int = 2
        override val usesDirectoryLayout: Boolean = false
        override fun resolve(cacheDirectory: File, segments: List<String>): File {
            require(segments.size == 2) { "FlatGroupArtifact requires 2 segments (group, artifact), got ${segments.size}" }
            val (group, artifact) = segments
            return cacheDirectory.resolve(group).resolve("$artifact.json")
        }
        override fun keyString(segments: List<String>): String = segments.joinToString(":")
    }
}
