package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.ValidationAction
import io.github.zenhelix.dependanger.core.model.ValidationSettings
import kotlinx.serialization.json.JsonElement

@DependangerDslMarker
public class SettingsDsl {
    private val _defaultDistribution = Trackable<String?>(null)
    public var defaultDistribution: String? by _defaultDistribution

    private val _strictVersionResolution = Trackable(false)
    public var strictVersionResolution: Boolean by _strictVersionResolution

    private val _repositories = Trackable<List<Repository>>(emptyList())
    public var repositories: List<Repository> by _repositories

    private val _validationSettings = Trackable(ValidationSettings.DEFAULT)
    public var validationSettings: ValidationSettings by _validationSettings

    private val _customSettings = Trackable<Map<String, JsonElement>>(emptyMap())
    public var customSettings: Map<String, JsonElement> by _customSettings

    public fun validation(block: ValidationSettingsDsl.() -> Unit) {
        val dsl = ValidationSettingsDsl().apply(block)
        validationSettings = dsl.toSettings()
    }

    public fun putCustomSetting(key: String, value: JsonElement) {
        customSettings = customSettings + (key to value)
    }

    public fun applyTo(target: SettingsDsl) {
        if (_defaultDistribution.isSet) target.defaultDistribution = defaultDistribution
        if (_strictVersionResolution.isSet) target.strictVersionResolution = strictVersionResolution
        if (_repositories.isSet) target.repositories = repositories
        if (_validationSettings.isSet) target.validationSettings = validationSettings
        if (_customSettings.isSet) target.customSettings = target.customSettings + customSettings
    }

    public fun mergeFrom(settings: Settings) {
        val defaults = Settings.DEFAULT
        if (settings.defaultDistribution != defaults.defaultDistribution) defaultDistribution = settings.defaultDistribution
        if (settings.strictVersionResolution != defaults.strictVersionResolution) strictVersionResolution = settings.strictVersionResolution
        if (settings.repositories != defaults.repositories) repositories = settings.repositories
        if (settings.validation != defaults.validation) validationSettings = settings.validation
        if (settings.customSettings != defaults.customSettings) customSettings = customSettings + settings.customSettings
    }

    public fun toSettings(): Settings = Settings(
        defaultDistribution = defaultDistribution,
        strictVersionResolution = strictVersionResolution,
        repositories = repositories,
        validation = validationSettings,
        customSettings = customSettings,
    )
}

@DependangerDslMarker
public class ValidationSettingsDsl {
    public var onCompatibilityViolation: ValidationAction = ValidationSettings.DEFAULT.onCompatibilityViolation
    public var onDeprecatedLibrary: ValidationAction = ValidationSettings.DEFAULT.onDeprecatedLibrary
    public var onJdkMismatch: ValidationAction = ValidationSettings.DEFAULT.onJdkMismatch
    public var onUnresolvedVersion: ValidationAction = ValidationSettings.DEFAULT.onUnresolvedVersion

    public fun toSettings(): ValidationSettings = ValidationSettings(
        onCompatibilityViolation = onCompatibilityViolation,
        onDeprecatedLibrary = onDeprecatedLibrary,
        onJdkMismatch = onJdkMismatch,
        onUnresolvedVersion = onUnresolvedVersion,
    )
}
