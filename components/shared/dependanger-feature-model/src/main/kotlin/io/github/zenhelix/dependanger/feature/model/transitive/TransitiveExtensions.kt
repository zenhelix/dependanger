package io.github.zenhelix.dependanger.feature.model.transitive

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension
import kotlinx.serialization.builtins.ListSerializer

public val TransitivesExtensionKey: ExtensionKey<List<TransitiveTree>> =
    ExtensionKey("transitives", ListSerializer(TransitiveTree.serializer()))

public val FlatDependenciesExtensionKey: ExtensionKey<List<FlatDependency>> =
    ExtensionKey("flatDependencies", ListSerializer(FlatDependency.serializer()))

public val VersionConflictsExtensionKey: ExtensionKey<List<VersionConflict>> =
    ExtensionKey("versionConflicts", ListSerializer(VersionConflict.serializer()))

public val EffectiveMetadata.transitives: List<TransitiveTree>
    get() = getExtension(TransitivesExtensionKey) ?: emptyList()

public val EffectiveMetadata.flatDependencies: List<FlatDependency>
    get() = getExtension(FlatDependenciesExtensionKey) ?: emptyList()

public val EffectiveMetadata.versionConflicts: List<VersionConflict>
    get() = getExtension(VersionConflictsExtensionKey) ?: emptyList()
