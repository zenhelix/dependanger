package io.github.zenhelix.dependanger.features.license

import kotlinx.serialization.Serializable

@Serializable
public enum class LicenseCategory {
    PERMISSIVE, WEAK_COPYLEFT, STRONG_COPYLEFT, PUBLIC_DOMAIN, PROPRIETARY, UNKNOWN
}
