package io.github.zenhelix.dependanger.effective.pipeline

public data class ProcessingEnvironment(
    val jdkVersion: Int? = null,
    val kotlinVersion: String? = null,
    val gradleVersion: String? = null,
    val environmentVariables: Map<String, String> = emptyMap(),
)
