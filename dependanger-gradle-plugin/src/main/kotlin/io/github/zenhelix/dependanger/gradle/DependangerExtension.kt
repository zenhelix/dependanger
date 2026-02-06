package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.core.dsl.BomImportsDsl
import io.github.zenhelix.dependanger.core.dsl.BundlesDsl
import io.github.zenhelix.dependanger.core.dsl.CompatibilityDsl
import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.DistributionsDsl
import io.github.zenhelix.dependanger.core.dsl.LibrariesDsl
import io.github.zenhelix.dependanger.core.dsl.PluginsDsl
import io.github.zenhelix.dependanger.core.dsl.PresetsDsl
import io.github.zenhelix.dependanger.core.dsl.ProcessingDsl
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.dsl.TargetPlatformsDsl
import io.github.zenhelix.dependanger.core.dsl.VersionsDsl
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

public open class DependangerExtension(project: Project) {
    public val dsl: DependangerDsl = DependangerDsl()

    public val outputDirectory: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("dependanger")
    )
    public val failOnError: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

    public fun versions(block: VersionsDsl.() -> Unit) {
        dsl.versions(block)
    }

    public fun libraries(block: LibrariesDsl.() -> Unit) {
        dsl.libraries(block)
    }

    public fun plugins(block: PluginsDsl.() -> Unit) {
        dsl.plugins(block)
    }

    public fun bundles(block: BundlesDsl.() -> Unit) {
        dsl.bundles(block)
    }

    public fun bomImports(block: BomImportsDsl.() -> Unit) {
        dsl.bomImports(block)
    }

    public fun targetPlatforms(block: TargetPlatformsDsl.() -> Unit) {
        dsl.targetPlatforms(block)
    }

    public fun distributions(block: DistributionsDsl.() -> Unit) {
        dsl.distributions(block)
    }

    public fun compatibility(block: CompatibilityDsl.() -> Unit) {
        dsl.compatibility(block)
    }

    public fun settings(block: SettingsDsl.() -> Unit) {
        dsl.settings(block)
    }

    public fun presets(block: PresetsDsl.() -> Unit) {
        dsl.presets(block)
    }

    public fun processing(block: ProcessingDsl.() -> Unit) {
        dsl.processing(block)
    }
}
