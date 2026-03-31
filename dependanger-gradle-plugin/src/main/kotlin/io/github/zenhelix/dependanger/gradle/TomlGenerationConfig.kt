package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import org.gradle.api.provider.Property

public abstract class TomlGenerationConfig {
    public abstract val filename: Property<String>
    public abstract val includeComments: Property<Boolean>
    public abstract val sortSections: Property<Boolean>
    public abstract val useInlineVersions: Property<Boolean>
    public abstract val includeDeprecationComments: Property<Boolean>

    init {
        filename.convention(TomlConfig.DEFAULT_FILENAME)
        includeComments.convention(true)
        sortSections.convention(true)
        useInlineVersions.convention(false)
        includeDeprecationComments.convention(true)
    }

    internal fun toConfig(): TomlConfig = TomlConfig(
        filename = filename.get(),
        includeComments = includeComments.get(),
        sortSections = sortSections.get(),
        useInlineVersions = useInlineVersions.get(),
        includeDeprecationComments = includeDeprecationComments.get(),
    )
}
