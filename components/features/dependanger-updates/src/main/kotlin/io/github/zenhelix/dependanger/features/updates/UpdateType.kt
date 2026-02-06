package io.github.zenhelix.dependanger.features.updates

import kotlinx.serialization.Serializable

@Serializable
public enum class UpdateType {
    MAJOR, MINOR, PATCH, PRERELEASE
}
