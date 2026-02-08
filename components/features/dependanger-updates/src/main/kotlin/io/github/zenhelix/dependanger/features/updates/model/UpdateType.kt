package io.github.zenhelix.dependanger.features.updates.model

import kotlinx.serialization.Serializable

@Serializable
public enum class UpdateType {
    MAJOR, MINOR, PATCH, PRERELEASE
}
