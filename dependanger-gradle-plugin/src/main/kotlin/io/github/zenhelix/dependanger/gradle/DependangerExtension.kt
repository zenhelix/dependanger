package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.DependangerDslApi
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

public open class DependangerExtension private constructor(
    project: Project,
    public val dsl: DependangerDsl,
) : DependangerDslApi by dsl {

    @Suppress("unused") // Gradle instantiation
    public constructor(project: Project) : this(project, DependangerDsl())

    public val outputDirectory: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("dependanger")
    )
    public val failOnError: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
}
