package io.github.zenhelix.dependanger.features.updates.model

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension

public val UpdatesExtensionKey: ExtensionKey<List<UpdateAvailableInfo>> = ExtensionKey("updates")

public val EffectiveMetadata.updates: List<UpdateAvailableInfo>
    get() = getExtension(UpdatesExtensionKey) ?: emptyList()
