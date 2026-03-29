package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree

internal fun tree(
    group: String,
    artifact: String,
    version: String?,
    scope: String? = "compile",
    children: List<TransitiveTree> = emptyList(),
    isDuplicate: Boolean = false,
    isCycle: Boolean = false,
): TransitiveTree = TransitiveTree(
    group = group,
    artifact = artifact,
    version = version,
    scope = scope,
    children = children,
    isDuplicate = isDuplicate,
    isCycle = isCycle,
)

internal fun effectiveLibrary(
    group: String,
    artifact: String,
): EffectiveLibrary = EffectiveLibrary(
    alias = "$group:$artifact",
    group = group,
    artifact = artifact,
    version = null,
    description = null,
    tags = emptySet(),
    requires = null,
    isDeprecated = false,
    deprecation = null,
    license = null,
    constraints = emptyList(),
    isPlatform = false,
)
