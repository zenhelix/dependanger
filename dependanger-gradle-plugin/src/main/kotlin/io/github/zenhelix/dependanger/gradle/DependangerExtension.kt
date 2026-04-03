package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.DependangerDslApi
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

public open class DependangerExtension private constructor(
    private val project: Project,
    private val dsl: DependangerDsl,
) : DependangerDslApi by dsl {

    @Suppress("unused") // Gradle instantiation
    public constructor(project: Project) : this(project, DependangerDsl())

    internal fun toMetadata() = dsl.toMetadata()

    public val outputDirectory: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("dependanger")
    )
    public val failOnError: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

    public val toml: TomlGenerationConfig = project.objects.newInstance(TomlGenerationConfig::class.java)
    public val bom: BomGenerationConfig = project.objects.newInstance(BomGenerationConfig::class.java)

    public fun toml(action: Action<TomlGenerationConfig>) {
        action.execute(toml)
    }

    public fun bom(action: Action<BomGenerationConfig>) {
        action.execute(bom)
    }
}
