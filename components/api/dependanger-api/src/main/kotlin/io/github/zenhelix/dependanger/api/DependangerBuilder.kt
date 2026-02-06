package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPreset

public class DependangerBuilder {
    private var metadata: DependangerMetadata? = null
    private var dslBlock: (DependangerDsl.() -> Unit)? = null
    private var preset: ProcessingPreset = ProcessingPreset.DEFAULT
    private var environment: ProcessingEnvironment = ProcessingEnvironment()

    public constructor(dslBlock: DependangerDsl.() -> Unit) {
        this.dslBlock = dslBlock
    }

    public constructor(metadata: DependangerMetadata) {
        this.metadata = metadata
    }

    public fun preset(preset: ProcessingPreset): DependangerBuilder = apply { this.preset = preset }
    public fun environment(environment: ProcessingEnvironment): DependangerBuilder = apply { this.environment = environment }
    public fun jdkVersion(version: Int): DependangerBuilder = apply { this.environment = environment.copy(jdkVersion = version) }
    public fun kotlinVersion(version: String): DependangerBuilder = apply { this.environment = environment.copy(kotlinVersion = version) }
    public fun gradleVersion(version: String): DependangerBuilder = apply { this.environment = environment.copy(gradleVersion = version) }

    public fun build(): Dependanger = TODO()
}
