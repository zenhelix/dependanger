package io.github.zenhelix.dependanger.features.license.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ClearlyDefinedDefinition(
    val licensed: ClearlyDefinedLicensed?,
)

@Serializable
internal data class ClearlyDefinedLicensed(
    val declared: String?,
)
