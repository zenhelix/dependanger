package io.github.zenhelix.dependanger.core.spi

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

public class SchemaMigrationChain(
    migrators: List<SchemaMigrator>,
) {
    private val migrationGraph: Map<String, SchemaMigrator> = migrators.associateBy { it.fromVersion }

    public fun canMigrate(fromVersion: String, toVersion: String): Boolean =
        findPath(fromVersion, toVersion) != null

    public fun migrate(metadata: DependangerMetadata, targetVersion: String): DependangerMetadata {
        if (metadata.schemaVersion == targetVersion) return metadata

        val path = findPath(metadata.schemaVersion, targetVersion)
            ?: throw SchemaMigrationException(
                "No migration path from ${metadata.schemaVersion} to $targetVersion. " +
                        "Available migrators: ${migrationGraph.keys.joinToString()}"
            )

        var result = metadata
        for (migrator in path) {
            result = migrator.migrate(result)
        }
        return result
    }

    public fun availableVersions(): Set<String> {
        val versions = mutableSetOf<String>()
        migrationGraph.values.forEach {
            versions.add(it.fromVersion)
            versions.add(it.toVersion)
        }
        return versions
    }

    private fun findPath(from: String, to: String): List<SchemaMigrator>? {
        if (from == to) return emptyList()

        val path = mutableListOf<SchemaMigrator>()
        val visited = mutableSetOf<String>()
        var current = from

        while (current != to) {
            if (current in visited) return null
            visited.add(current)

            val migrator = migrationGraph[current] ?: return null
            path.add(migrator)
            current = migrator.toVersion
        }

        return path
    }
}

public class SchemaMigrationException(
    message: String,
) : RuntimeException(message)
