package io.github.zenhelix.dependanger.features.license.model

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension

public val LicenseViolationsExtensionKey: ExtensionKey<List<LicenseViolation>> = ExtensionKey("licenseViolations")

public val EffectiveMetadata.licenseViolations: List<LicenseViolation>
    get() = getExtension(LicenseViolationsExtensionKey) ?: emptyList()
