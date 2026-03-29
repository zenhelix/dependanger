package io.github.zenhelix.dependanger.feature.model.license

import kotlinx.serialization.Serializable

@Serializable
public enum class LicenseViolationType {
    DENIED, NOT_ALLOWED
}

@Serializable
public data class LicenseViolation(
    val alias: String,
    val group: String,
    val artifact: String,
    val detectedLicense: String?,
    val category: LicenseCategory,
    val violationType: LicenseViolationType,
    val message: String,
)
