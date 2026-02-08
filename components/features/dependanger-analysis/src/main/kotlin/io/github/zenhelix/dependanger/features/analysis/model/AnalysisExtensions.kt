package io.github.zenhelix.dependanger.features.analysis.model

import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension

public val CompatibilityIssuesExtensionKey: ExtensionKey<List<CompatibilityIssue>> = ExtensionKey("compatibilityIssues")

public val EffectiveMetadata.compatibilityIssues: List<CompatibilityIssue>
    get() = getExtension(CompatibilityIssuesExtensionKey) ?: emptyList()
