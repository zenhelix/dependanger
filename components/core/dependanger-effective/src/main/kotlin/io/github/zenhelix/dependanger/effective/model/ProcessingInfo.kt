package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public data class ProcessingInfo(
    val processedAt: String,
    val processorIds: List<String> = emptyList(),
    val environment: ProcessingEnvironmentSnapshot = ProcessingEnvironmentSnapshot(),
)

@Serializable
public data class ProcessingEnvironmentSnapshot(
    val jdkVersion: Int? = null,
    val kotlinVersion: String? = null,
    val gradleVersion: String? = null,
)
