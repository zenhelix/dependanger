package io.github.zenhelix.dependanger.core.model.metadata

import io.github.zenhelix.dependanger.core.model.BomImport
import io.github.zenhelix.dependanger.core.model.Bundle
import io.github.zenhelix.dependanger.core.model.CompatibilityRule
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
    val schemaVersion: String = "1.0",
    val versions: List<Version> = emptyList(),
    val libraries: List<Library> = emptyList(),
    val plugins: List<Plugin> = emptyList(),
    val bundles: List<Bundle> = emptyList(),
    val bomImports: List<BomImport> = emptyList(),
    val targetPlatforms: List<TargetPlatform> = emptyList(),
    val distributions: List<Distribution> = emptyList(),
    val compatibility: List<CompatibilityRule> = emptyList(),
    val settings: Settings = Settings(),
    val presets: List<Preset> = emptyList(),
)
