package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.BomImport
import io.github.zenhelix.dependanger.core.model.VersionReference

@DependangerDslMarker
public class BomImportsDsl {
    public val boms: MutableList<BomImport> = mutableListOf()

    public fun bom(alias: String, coordinates: String) {
        val parts = coordinates.split(":")
        when (parts.size) {
            2    -> throw IllegalArgumentException("BOM import requires a version: $coordinates")
            3    -> boms.add(BomImport(alias = alias, group = parts[0], name = parts[1], version = VersionReference.Literal(parts[2])))
            else -> throw IllegalArgumentException("Invalid BOM coordinates: $coordinates")
        }
    }

    public fun bom(alias: String, coordinates: String, version: VersionReference) {
        val parts = coordinates.split(":")
        require(parts.size == 2) { "Coordinates with version parameter should be group:artifact" }
        boms.add(BomImport(alias = alias, group = parts[0], name = parts[1], version = version))
    }

    public fun bom(alias: String, group: String, artifact: String, version: VersionReference) {
        boms.add(BomImport(alias = alias, group = group, name = artifact, version = version))
    }
}
