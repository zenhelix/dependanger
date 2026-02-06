package io.github.zenhelix.dependanger.core.validation

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

public interface DslValidator {
    public fun validate(metadata: DependangerMetadata): ValidationResult
}
