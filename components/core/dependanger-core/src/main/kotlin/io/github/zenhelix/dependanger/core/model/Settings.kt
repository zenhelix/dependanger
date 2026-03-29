package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class Settings(
    val defaultDistribution: String?,
    val strictVersionResolution: Boolean,
    val repositories: List<Repository>,
    val validation: ValidationSettings,
    val customSettings: Map<String, JsonElement>,
) {
    public companion object {
        public val DEFAULT: Settings = Settings(
            defaultDistribution = null,
            strictVersionResolution = false,
            repositories = emptyList(),
            validation = ValidationSettings.DEFAULT,
            customSettings = emptyMap(),
        )
    }
}

@Serializable
public data class ValidationSettings(
    val onCompatibilityViolation: ValidationAction,
    val onDeprecatedLibrary: ValidationAction,
    val onJdkMismatch: ValidationAction,
    val onUnresolvedVersion: ValidationAction,
) {
    public companion object {
        public val DEFAULT: ValidationSettings = ValidationSettings(
            onCompatibilityViolation = ValidationAction.FAIL,
            onDeprecatedLibrary = ValidationAction.WARN,
            onJdkMismatch = ValidationAction.FAIL,
            onUnresolvedVersion = ValidationAction.WARN,
        )
    }
}
