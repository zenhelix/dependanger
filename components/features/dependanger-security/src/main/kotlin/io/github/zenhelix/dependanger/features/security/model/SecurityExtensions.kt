package io.github.zenhelix.dependanger.features.security.model

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension

public val VulnerabilitiesExtensionKey: ExtensionKey<List<VulnerabilityInfo>> = ExtensionKey("vulnerabilities")

public val EffectiveMetadata.vulnerabilities: List<VulnerabilityInfo>
    get() = getExtension(VulnerabilitiesExtensionKey) ?: emptyList()
