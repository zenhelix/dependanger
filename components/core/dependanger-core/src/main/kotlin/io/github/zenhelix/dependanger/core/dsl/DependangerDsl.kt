package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

@DependangerDslMarker
public class DependangerDsl {
    public val versionsDsl: VersionsDsl = VersionsDsl()
    public val librariesDsl: LibrariesDsl = LibrariesDsl()
    public val pluginsDsl: PluginsDsl = PluginsDsl()
    public val bundlesDsl: BundlesDsl = BundlesDsl()
    public val bomImportsDsl: BomImportsDsl = BomImportsDsl()
    public val targetPlatformsDsl: TargetPlatformsDsl = TargetPlatformsDsl()
    public val distributionsDsl: DistributionsDsl = DistributionsDsl()
    public val compatibilityDsl: CompatibilityDsl = CompatibilityDsl()
    public val settingsDsl: SettingsDsl = SettingsDsl()
    public val presetsDsl: PresetsDsl = PresetsDsl()
    public val processingDsl: ProcessingDsl = ProcessingDsl()

    public fun versions(block: VersionsDsl.() -> Unit) {
        versionsDsl.apply(block)
    }

    public fun libraries(block: LibrariesDsl.() -> Unit) {
        librariesDsl.apply(block)
    }

    public fun plugins(block: PluginsDsl.() -> Unit) {
        pluginsDsl.apply(block)
    }

    public fun bundles(block: BundlesDsl.() -> Unit) {
        bundlesDsl.apply(block)
    }

    public fun bomImports(block: BomImportsDsl.() -> Unit) {
        bomImportsDsl.apply(block)
    }

    public fun targetPlatforms(block: TargetPlatformsDsl.() -> Unit) {
        targetPlatformsDsl.apply(block)
    }

    public fun distributions(block: DistributionsDsl.() -> Unit) {
        distributionsDsl.apply(block)
    }

    public fun compatibility(block: CompatibilityDsl.() -> Unit) {
        compatibilityDsl.apply(block)
    }

    public fun settings(block: SettingsDsl.() -> Unit) {
        settingsDsl.apply(block)
    }

    public fun presets(block: PresetsDsl.() -> Unit) {
        presetsDsl.apply(block)
    }

    public fun processing(block: ProcessingDsl.() -> Unit) {
        processingDsl.apply(block)
    }

    public fun applyPreset(name: String): Unit = TODO()

    public fun toMetadata(): DependangerMetadata = TODO()
}

public fun dependanger(block: DependangerDsl.() -> Unit): DependangerDsl =
    DependangerDsl().apply(block)
