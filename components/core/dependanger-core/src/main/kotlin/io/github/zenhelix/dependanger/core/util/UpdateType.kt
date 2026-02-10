package io.github.zenhelix.dependanger.core.util

import kotlinx.serialization.Serializable

@Serializable
public enum class UpdateType { PRERELEASE, PATCH, MINOR, MAJOR }
