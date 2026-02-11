package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class TargetPlatform(
    val name: String,
    val jdkConstraints: JdkConstraints?,
    val androidConstraints: AndroidConstraints?,
    val kotlinConstraints: KotlinConstraints?,
    val supportedTargets: Set<KmpTarget>,
)
