package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.Diagnostics
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public data class EffectiveMetadata(
    val schemaVersion: String,
    val distribution: String?,
    val versions: Map<String, ResolvedVersion>,
    val libraries: Map<String, EffectiveLibrary>,
    val plugins: Map<String, EffectivePlugin>,
    val bundles: Map<String, EffectiveBundle>,
    val diagnostics: Diagnostics,
    val processingInfo: ProcessingInfo?,
    @Transient
    val extensions: Map<ExtensionKey<*>, Any> = emptyMap(),
)

public fun EffectiveMetadata.withDiagnostic(d: Diagnostics): EffectiveMetadata =
    copy(diagnostics = diagnostics + d)
