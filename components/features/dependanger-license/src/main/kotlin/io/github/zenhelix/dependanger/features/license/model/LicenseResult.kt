package io.github.zenhelix.dependanger.features.license.model

import kotlinx.serialization.Serializable

@Serializable
public data class LicenseResult(
    val spdxId: String?,
    val licenseName: String?,
    val source: LicenseSource,
    val category: LicenseCategory,
)
