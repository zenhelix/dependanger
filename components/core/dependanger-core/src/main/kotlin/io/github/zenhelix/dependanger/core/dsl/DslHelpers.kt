package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.VersionRange
import io.github.zenhelix.dependanger.core.model.VersionReference

public fun versionRef(name: String): VersionReference = VersionReference.Reference(name)

public fun versionRange(notation: String): VersionReference = VersionReference.Range(VersionRange(notation))
