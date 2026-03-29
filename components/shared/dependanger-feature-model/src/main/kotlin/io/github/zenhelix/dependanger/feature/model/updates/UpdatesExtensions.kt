package io.github.zenhelix.dependanger.feature.model.updates

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension
import kotlinx.serialization.builtins.ListSerializer

public val UpdatesExtensionKey: ExtensionKey<List<UpdateAvailableInfo>> =
    ExtensionKey("updates", ListSerializer(UpdateAvailableInfo.serializer()))

public val EffectiveMetadata.updates: List<UpdateAvailableInfo>
    get() = getExtension(UpdatesExtensionKey) ?: emptyList()
