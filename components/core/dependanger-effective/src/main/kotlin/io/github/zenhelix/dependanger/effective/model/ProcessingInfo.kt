package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public data class ProcessingInfo(
    val processedAt: String,
    val processorIds: List<String>,
    val registeredProcessorIds: List<String>,
    val skippedProcessorIds: List<String>,
    val environment: ProcessingEnvironmentSnapshot,
)

@Serializable
public data class ProcessingEnvironmentSnapshot(
    val jdkVersion: Int?,
    val kotlinVersion: String?,
    val gradleVersion: String?,
)
