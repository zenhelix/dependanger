package io.github.zenhelix.dependanger.feature.model.license

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import kotlinx.serialization.Serializable

@Serializable
public enum class LicenseViolationType {
    DENIED, NOT_ALLOWED
}

@Serializable
public data class LicenseViolation(
    val alias: String,
    val coordinate: MavenCoordinate,
    val detectedLicense: String?,
    val category: LicenseCategory,
    val violationType: LicenseViolationType,
    val message: String,
)
