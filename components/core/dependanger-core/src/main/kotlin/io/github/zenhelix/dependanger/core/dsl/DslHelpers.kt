package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.VersionReference

public fun versionRef(name: String): VersionReference = VersionReference.Reference(name)

public fun versionRange(notation: String): VersionReference = VersionReference.Range(VersionReference.VersionRange.Simple(notation))

public fun richVersion(
    require: String? = null,
    strictly: String? = null,
    prefer: String? = null,
    reject: List<String> = emptyList(),
): VersionReference = VersionReference.Range(
    VersionReference.VersionRange.Rich(require = require, strictly = strictly, prefer = prefer, reject = reject)
)
