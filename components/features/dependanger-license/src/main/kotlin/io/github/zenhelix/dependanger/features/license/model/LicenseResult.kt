package io.github.zenhelix.dependanger.features.license.model

import kotlinx.serialization.Serializable

@Serializable
public data class LicenseResult(
    val spdxId: String? = null,
    val licenseName: String? = null,
    val source: LicenseSource = LicenseSource.UNKNOWN,
    val category: LicenseCategory = LicenseCategory.UNKNOWN,
)
