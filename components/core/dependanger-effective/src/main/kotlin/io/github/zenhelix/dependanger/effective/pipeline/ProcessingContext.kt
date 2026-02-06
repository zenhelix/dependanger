package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

public data class ProcessingContext(
    val originalMetadata: DependangerMetadata,
    val settings: Settings,
    val environment: ProcessingEnvironment,
    val activeDistribution: String? = null,
    val properties: MutableMap<String, Any> = mutableMapOf(),
)
