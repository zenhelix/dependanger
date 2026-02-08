package io.github.zenhelix.dependanger.features.license.model

import kotlinx.serialization.Serializable

@Serializable
public enum class LicenseSource {
    MAVEN_POM, CLEARLY_DEFINED, DECLARED, UNKNOWN
}
