package io.github.zenhelix.dependanger.effective.model

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
