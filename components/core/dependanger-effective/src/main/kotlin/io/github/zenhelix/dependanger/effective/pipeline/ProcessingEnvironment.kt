package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.model.ProcessingEnvironmentSnapshot

public data class ProcessingEnvironment(
    val jdkVersion: Int?,
    val kotlinVersion: String?,
    val gradleVersion: String?,
    val environmentVariables: Map<String, String>,
) {
    public fun toSnapshot(): ProcessingEnvironmentSnapshot = ProcessingEnvironmentSnapshot(
        jdkVersion = jdkVersion,
        kotlinVersion = kotlinVersion,
        gradleVersion = gradleVersion,
    )
}
