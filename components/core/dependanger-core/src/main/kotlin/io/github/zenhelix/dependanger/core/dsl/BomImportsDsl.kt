package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.BomImport
import io.github.zenhelix.dependanger.core.model.VersionReference

@DependangerDslMarker
public class BomImportsDsl {
    private val _boms: MutableList<BomImport> = mutableListOf()
    public val boms: List<BomImport> get() = _boms.toList()

    public fun bom(alias: String, coordinates: String) {
        require(alias.isNotBlank()) { "BOM alias must not be blank" }
        require(_boms.none { it.alias == alias }) { "Duplicate BOM alias: '$alias'" }
        val parts = coordinates.split(":")
        when (parts.size) {
            2    -> throw IllegalArgumentException("BOM import requires a version: $coordinates")
            3 -> _boms.add(BomImport(alias = alias, group = parts[0], artifact = parts[1], version = VersionReference.Literal(parts[2])))
            else -> throw IllegalArgumentException("Invalid BOM coordinates: $coordinates")
        }
    }

    public fun bom(alias: String, coordinates: String, version: VersionReference) {
        require(alias.isNotBlank()) { "BOM alias must not be blank" }
        require(_boms.none { it.alias == alias }) { "Duplicate BOM alias: '$alias'" }
        val parts = coordinates.split(":")
        require(parts.size == 2) { "Coordinates with version parameter should be group:artifact" }
        _boms.add(BomImport(alias = alias, group = parts[0], artifact = parts[1], version = version))
    }

    public fun bom(alias: String, group: String, artifact: String, version: VersionReference) {
        require(alias.isNotBlank()) { "BOM alias must not be blank" }
        require(_boms.none { it.alias == alias }) { "Duplicate BOM alias: '$alias'" }
        _boms.add(BomImport(alias = alias, group = group, artifact = artifact, version = version))
    }
}
