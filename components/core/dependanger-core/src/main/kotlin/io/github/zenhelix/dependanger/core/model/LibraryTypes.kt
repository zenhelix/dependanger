package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class DeprecationInfo(
    val replacedBy: String?,
    val message: String?,
    val since: String?,
    val removalVersion: String?,
)

@Serializable
public data class Requirements(
    val jdk: JdkConstraints?,
    val kotlin: KotlinConstraints?,
)

@Serializable
public data class JdkConstraints(
    val min: Int?,
    val max: Int?,
)

@Serializable
public data class KotlinConstraints(
    val min: String?,
    val max: String?,
)

@Serializable
public data class AndroidConstraints(
    val minSdk: Int?,
    val targetSdk: Int?,
)

@Serializable
public data class LicenseInfo(
    val id: String?,
    val url: String?,
)
