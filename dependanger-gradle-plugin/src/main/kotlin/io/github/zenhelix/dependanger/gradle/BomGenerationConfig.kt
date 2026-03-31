package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.generators.bom.BomConfig
import org.gradle.api.provider.Property

public abstract class BomGenerationConfig {
    public abstract val groupId: Property<String>
    public abstract val artifactId: Property<String>
    public abstract val version: Property<String>
    public abstract val name: Property<String>
    public abstract val description: Property<String>
    public abstract val filename: Property<String>
    public abstract val includeOptionalDependencies: Property<Boolean>
    public abstract val prettyPrint: Property<Boolean>
    public abstract val includeDeprecationComments: Property<Boolean>

    init {
        filename.convention(BomConfig.DEFAULT_FILENAME)
        includeOptionalDependencies.convention(false)
        prettyPrint.convention(true)
        includeDeprecationComments.convention(true)
    }

    internal fun toConfig(
        fallbackGroupId: String,
        fallbackArtifactId: String,
        fallbackVersion: String,
    ): BomConfig = BomConfig(
        groupId = groupId.getOrElse(fallbackGroupId),
        artifactId = artifactId.getOrElse(fallbackArtifactId),
        version = version.getOrElse(fallbackVersion),
        name = name.orNull,
        description = description.orNull,
        filename = filename.get(),
        includeOptionalDependencies = includeOptionalDependencies.get(),
        prettyPrint = prettyPrint.get(),
        includeDeprecationComments = includeDeprecationComments.get(),
    )
}
