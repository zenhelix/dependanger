package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public enum class UpdateType {
    MAJOR, MINOR, PATCH, PRERELEASE
}
