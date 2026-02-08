package io.github.zenhelix.dependanger.features.transitive.model

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension

public val TransitivesExtensionKey: ExtensionKey<List<TransitiveTree>> = ExtensionKey("transitives")

public val FlatDependenciesExtensionKey: ExtensionKey<List<FlatDependency>> = ExtensionKey("flatDependencies")

public val VersionConflictsExtensionKey: ExtensionKey<List<VersionConflict>> = ExtensionKey("versionConflicts")

public val EffectiveMetadata.transitives: List<TransitiveTree>
    get() = getExtension(TransitivesExtensionKey) ?: emptyList()

public val EffectiveMetadata.flatDependencies: List<FlatDependency>
    get() = getExtension(FlatDependenciesExtensionKey) ?: emptyList()

public val EffectiveMetadata.versionConflicts: List<VersionConflict>
    get() = getExtension(VersionConflictsExtensionKey) ?: emptyList()
