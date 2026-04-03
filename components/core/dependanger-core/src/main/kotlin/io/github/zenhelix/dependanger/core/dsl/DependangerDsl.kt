package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

@DependangerDslMarker
public class DependangerDsl : DependangerDslApi {
    private val versionsDsl: VersionsDsl = VersionsDsl()
    private val librariesDsl: LibrariesDsl = LibrariesDsl()
    private val pluginsDsl: PluginsDsl = PluginsDsl()
    private val bundlesDsl: BundlesDsl = BundlesDsl()
    private val bomImportsDsl: BomImportsDsl = BomImportsDsl()
    private val targetPlatformsDsl: TargetPlatformsDsl = TargetPlatformsDsl()
    private val distributionsDsl: DistributionsDsl = DistributionsDsl()
    private val constraintsDsl: ConstraintsDsl = ConstraintsDsl()
    private val compatibilityDsl: CompatibilityDsl = CompatibilityDsl()
    private val settingsDsl: SettingsDsl = SettingsDsl()
    private val presetsDsl: PresetsDsl = PresetsDsl()
    private val processingDsl: ProcessingDsl = ProcessingDsl()

    public override fun versions(block: VersionsDsl.() -> Unit) {
        versionsDsl.apply(block)
    }

    public override fun libraries(block: LibrariesDsl.() -> Unit) {
        librariesDsl.apply(block)
    }

    public override fun plugins(block: PluginsDsl.() -> Unit) {
        pluginsDsl.apply(block)
    }

    public override fun bundles(block: BundlesDsl.() -> Unit) {
        bundlesDsl.apply(block)
    }

    public override fun bomImports(block: BomImportsDsl.() -> Unit) {
        bomImportsDsl.apply(block)
    }

    public override fun targetPlatforms(block: TargetPlatformsDsl.() -> Unit) {
        targetPlatformsDsl.apply(block)
    }

    public override fun distributions(block: DistributionsDsl.() -> Unit) {
        distributionsDsl.apply(block)
    }

    public override fun constraints(block: ConstraintsDsl.() -> Unit) {
        constraintsDsl.apply(block)
    }

    public override fun compatibility(block: CompatibilityDsl.() -> Unit) {
        compatibilityDsl.apply(block)
    }

    public override fun settings(block: SettingsDsl.() -> Unit) {
        settingsDsl.apply(block)
    }

    public override fun presets(block: PresetsDsl.() -> Unit) {
        presetsDsl.apply(block)
    }

    public override fun processing(block: ProcessingDsl.() -> Unit) {
        processingDsl.apply(block)
    }

    public fun applyPreset(name: String) {
        // Presets reference bundles/distributions by name rather than embedding them,
        // so consumers (API, CLI, Gradle Plugin) can resolve scope independently.

        val presetDsl = presetsDsl.findDslByName(name)
        if (presetDsl != null) {
            // DSL-defined preset: precise merge via Trackable — only explicitly set fields are applied.
            presetDsl.settingsDsl?.applyTo(settingsDsl)
            return
        }

        // Fallback for JSON-loaded presets that have no DSL object (e.g., imported from file).
        val preset = presetsDsl.findByName(name)
            ?: throw IllegalArgumentException(
                "Preset '$name' not found. Available presets: ${presetsDsl.availableNames()}"
            )
        preset.settings?.let { presetSettings ->
            settingsDsl.mergeFrom(presetSettings)
        }
    }

    public fun toMetadata(): DependangerMetadata = DependangerMetadata(
        schemaVersion = DependangerMetadata.SCHEMA_VERSION,
        versions = versionsDsl.versions.toList(),
        libraries = librariesDsl.libraries.toList(),
        plugins = pluginsDsl.plugins.toList(),
        bundles = bundlesDsl.bundles.toList(),
        bomImports = bomImportsDsl.boms.toList(),
        constraints = constraintsDsl.constraints.toList(),
        targetPlatforms = targetPlatformsDsl.platforms.toList(),
        distributions = distributionsDsl.distributions.toList(),
        compatibility = compatibilityDsl.rules.toList(),
        settings = settingsDsl.toSettings(),
        presets = presetsDsl.presets.toList(),
    )
}

public fun dependanger(block: DependangerDsl.() -> Unit): DependangerDsl =
    DependangerDsl().apply(block)
