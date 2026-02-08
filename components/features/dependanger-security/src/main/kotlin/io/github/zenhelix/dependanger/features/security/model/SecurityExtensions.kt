package io.github.zenhelix.dependanger.features.security.model

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension
import kotlinx.serialization.builtins.ListSerializer

public val VulnerabilitiesExtensionKey: ExtensionKey<List<VulnerabilityInfo>> =
    ExtensionKey("vulnerabilities", ListSerializer(VulnerabilityInfo.serializer()))

public val EffectiveMetadata.vulnerabilities: List<VulnerabilityInfo>
    get() = getExtension(VulnerabilitiesExtensionKey) ?: emptyList()
