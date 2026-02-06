package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class TargetPlatform(
    val name: String,
    val jdkConstraints: JdkConstraints? = null,
    val androidConstraints: AndroidConstraints? = null,
    val kotlinConstraints: KotlinConstraints? = null,
    val supportedTargets: Set<KmpTarget> = emptySet(),
)
