package io.github.zenhelix.dependanger.feature.model.license

import kotlinx.serialization.Serializable

@Serializable
public enum class LicenseCategory {
    PERMISSIVE, WEAK_COPYLEFT, STRONG_COPYLEFT, PUBLIC_DOMAIN, PROPRIETARY, UNKNOWN
}

public val LicenseCategory.isCopyleft: Boolean
    get() = this == LicenseCategory.WEAK_COPYLEFT || this == LicenseCategory.STRONG_COPYLEFT
