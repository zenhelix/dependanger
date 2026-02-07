package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.model.ProcessingEnvironmentSnapshot

public data class ProcessingEnvironment(
    val jdkVersion: Int? = null,
    val kotlinVersion: String? = null,
    val gradleVersion: String? = null,
    val environmentVariables: Map<String, String> = emptyMap(),
) {
    public fun toSnapshot(): ProcessingEnvironmentSnapshot = ProcessingEnvironmentSnapshot(
        jdkVersion = jdkVersion,
        kotlinVersion = kotlinVersion,
        gradleVersion = gradleVersion,
    )
}
