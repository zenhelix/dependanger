package io.github.zenhelix.dependanger.features.license.model

import kotlinx.serialization.Serializable

@Serializable
public data class LicenseViolation(
    val alias: String,
    val group: String,
    val artifact: String,
    val detectedLicense: String?,
    val category: LicenseCategory,
    val message: String,
)
