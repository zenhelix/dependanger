package io.github.zenhelix.dependanger.core.dsl

/**
 * Public DSL API for configuring Dependanger metadata.
 * Implemented by [DependangerDsl] and used for delegation in Gradle plugin's DependangerExtension.
 */
@DependangerDslMarker
public interface DependangerDslApi {
    public fun versions(block: VersionsDsl.() -> Unit)
    public fun libraries(block: LibrariesDsl.() -> Unit)
    public fun plugins(block: PluginsDsl.() -> Unit)
    public fun bundles(block: BundlesDsl.() -> Unit)
    public fun bomImports(block: BomImportsDsl.() -> Unit)
    public fun targetPlatforms(block: TargetPlatformsDsl.() -> Unit)
    public fun distributions(block: DistributionsDsl.() -> Unit)
    public fun constraints(block: ConstraintsDsl.() -> Unit)
    public fun compatibility(block: CompatibilityDsl.() -> Unit)
    public fun settings(block: SettingsDsl.() -> Unit)
    public fun presets(block: PresetsDsl.() -> Unit)
    public fun processing(block: ProcessingDsl.() -> Unit)
}
