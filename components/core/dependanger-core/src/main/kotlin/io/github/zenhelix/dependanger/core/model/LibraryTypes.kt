package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class DeprecationInfo(
    val replacedBy: String? = null,
    val message: String? = null,
    val since: String? = null,
    val removalVersion: String? = null,
)

@Serializable
public data class Requirements(
    val jdk: JdkConstraints? = null,
    val kotlin: KotlinConstraints? = null,
)

@Serializable
public data class JdkConstraints(
    val min: Int? = null,
    val max: Int? = null,
)

@Serializable
public data class KotlinConstraints(
    val min: String? = null,
    val max: String? = null,
)

@Serializable
public data class AndroidConstraints(
    val minSdk: Int? = null,
    val targetSdk: Int? = null,
)

@Serializable
public data class LicenseInfo(
    val id: String? = null,
    val url: String? = null,
)
