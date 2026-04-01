package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public enum class ProcessingPreset {
    DEFAULT, MINIMAL, STRICT
}
