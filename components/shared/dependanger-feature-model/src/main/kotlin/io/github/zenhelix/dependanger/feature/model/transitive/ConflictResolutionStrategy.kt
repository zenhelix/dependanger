package io.github.zenhelix.dependanger.feature.model.transitive

import kotlinx.serialization.Serializable

@Serializable
public enum class ConflictResolutionStrategy {
    HIGHEST, FIRST, FAIL, CONSTRAINT
}
