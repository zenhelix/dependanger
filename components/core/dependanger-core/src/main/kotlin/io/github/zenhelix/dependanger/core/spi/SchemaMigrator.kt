package io.github.zenhelix.dependanger.core.spi

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

/**
 * SPI for migrating [DependangerMetadata] between schema versions.
 *
 * Implementations handle migration from one specific schema version to another.
 * Multiple migrators can be chained to migrate across several versions:
 * e.g. 1.0 → 1.1 → 2.0.
 */
public interface SchemaMigrator {
    /** Schema version this migrator reads from. */
    public val fromVersion: String

    /** Schema version this migrator produces. */
    public val toVersion: String

    /** Migrate metadata from [fromVersion] to [toVersion]. */
    public fun migrate(metadata: DependangerMetadata): DependangerMetadata
}
