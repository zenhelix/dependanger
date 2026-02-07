package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

public class DslExtensionKey<T : Any>(public val name: String) {
    override fun equals(other: Any?): Boolean = other is DslExtensionKey<*> && name == other.name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = "DslExtensionKey($name)"
}

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
    private val extensions: MutableMap<DslExtensionKey<*>, Any> = mutableMapOf()

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

    public fun <T : Any> registerExtension(key: DslExtensionKey<T>, extension: T) {
        extensions[key] = extension
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> extension(key: DslExtensionKey<T>): T? = extensions[key] as? T

    public fun allExtensions(): Map<DslExtensionKey<*>, Any> = extensions.toMap()

    public fun applyPreset(name: String): Unit = TODO()

    public fun toMetadata(): DependangerMetadata = TODO()
}

public fun <T : Any> DependangerDsl.configure(key: DslExtensionKey<T>, block: T.() -> Unit) {
    val ext = extension(key) ?: error("DSL extension '${key.name}' is not registered. Call registerExtension() first.")
    ext.block()
}

public fun dependanger(block: DependangerDsl.() -> Unit): DependangerDsl =
    DependangerDsl().apply(block)
