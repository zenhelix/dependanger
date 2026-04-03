package io.github.zenhelix.dependanger.core.model.metadata

import io.github.zenhelix.dependanger.core.model.BomImport
import io.github.zenhelix.dependanger.core.model.Bundle
import io.github.zenhelix.dependanger.core.model.CompatibilityRule
import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.Distribution
import io.github.zenhelix.dependanger.core.model.Library
import io.github.zenhelix.dependanger.core.model.Plugin
import io.github.zenhelix.dependanger.core.model.Preset
import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.TargetPlatform
import io.github.zenhelix.dependanger.core.model.Version
import kotlinx.serialization.Serializable

@Serializable
public data class DependangerMetadata(
    val schemaVersion: String,
    val versions: List<Version>,
    val libraries: List<Library>,
    val plugins: List<Plugin>,
    val bundles: List<Bundle>,
    val bomImports: List<BomImport>,
    val constraints: List<Constraint>,
    val targetPlatforms: List<TargetPlatform>,
    val distributions: List<Distribution>,
    val compatibility: List<CompatibilityRule>,
    val settings: Settings,
    val presets: List<Preset>,
) {
    public companion object {
        public const val SCHEMA_VERSION: String = "1.0"
    }
}
