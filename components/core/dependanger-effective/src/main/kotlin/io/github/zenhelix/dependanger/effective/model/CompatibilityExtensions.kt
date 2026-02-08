package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.builtins.ListSerializer

public val CompatibilityIssuesExtensionKey: ExtensionKey<List<CompatibilityIssue>> =
    ExtensionKey("compatibilityIssues", ListSerializer(CompatibilityIssue.serializer()))

public val EffectiveMetadata.compatibilityIssues: List<CompatibilityIssue>
    get() = getExtension(CompatibilityIssuesExtensionKey) ?: emptyList()
