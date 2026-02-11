package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.ValidationAction
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class ValidationProcessor : EffectiveMetadataProcessor {
    override val id: String = "validation"
    override val phase: ProcessingPhase = ProcessingPhase.VALIDATION

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val validationSettings = context.settings.validation
        var diagnostics = metadata.diagnostics

        diagnostics = diagnostics + validateDuplicateAliases(metadata)

        diagnostics = diagnostics + validateUnresolvedVersions(
            metadata, context, validationSettings.onUnresolvedVersion,
        )

        diagnostics = diagnostics + validateBundleReferences(metadata)

        diagnostics = diagnostics + validateCircularExtends(context.originalMetadata)

        diagnostics = diagnostics + validateDeprecatedReferences(
            metadata, validationSettings.onDeprecatedLibrary,
        )

        diagnostics = diagnostics + validateCoordinates(metadata)

        return metadata.copy(diagnostics = diagnostics)
    }

    private fun validateDuplicateAliases(metadata: EffectiveMetadata): Diagnostics {
        val allAliases = mutableMapOf<String, MutableList<String>>()
        metadata.libraries.keys.forEach { allAliases.getOrPut(it) { mutableListOf() }.add("library") }
        metadata.plugins.keys.forEach { allAliases.getOrPut(it) { mutableListOf() }.add("plugin") }
        metadata.bundles.keys.forEach { allAliases.getOrPut(it) { mutableListOf() }.add("bundle") }

        val duplicates = allAliases.filter { it.value.size > 1 }
        val messages = duplicates.map { (alias, types) ->
            DiagnosticMessage(
                code = "VALIDATION_DUPLICATE_ALIAS",
                message = "Alias '$alias' used in multiple namespaces: ${types.joinToString()}",
                severity = Severity.ERROR,
                processorId = "validation",
                context = emptyMap(),
            )
        }
        return Diagnostics(
            errors = messages,
            warnings = emptyList(),
            infos = emptyList(),
        )
    }

    private fun validateUnresolvedVersions(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        action: ValidationAction,
    ): Diagnostics {
        if (action == ValidationAction.IGNORE) return Diagnostics(
            errors = emptyList(),
            warnings = emptyList(),
            infos = emptyList(),
        )

        val originalLibs = context.originalMetadata.libraries.associateBy { it.alias }
        val issues = mutableListOf<DiagnosticMessage>()

        for ((alias, lib) in metadata.libraries) {
            val originalRef = originalLibs[alias]?.version
            if (originalRef is VersionReference.Reference && lib.version == null) {
                issues.add(
                    DiagnosticMessage(
                        code = "VALIDATION_UNRESOLVED_REF",
                        message = "Library '$alias': version reference '${originalRef.name}' is not resolved",
                        severity = action.toSeverity(),
                        processorId = "validation",
                        context = emptyMap(),
                    )
                )
            }
        }
        return Diagnostics.of(*issues.toTypedArray())
    }

    private fun validateBundleReferences(metadata: EffectiveMetadata): Diagnostics {
        val issues = mutableListOf<DiagnosticMessage>()
        for ((bundleAlias, bundle) in metadata.bundles) {
            for (libAlias in bundle.libraries) {
                if (libAlias !in metadata.libraries) {
                    issues.add(
                        DiagnosticMessage(
                            code = "VALIDATION_BUNDLE_REF_MISSING",
                            message = "Bundle '$bundleAlias': library '$libAlias' does not exist",
                            severity = Severity.ERROR,
                            processorId = "validation",
                            context = emptyMap(),
                        )
                    )
                }
            }
        }
        return Diagnostics.of(*issues.toTypedArray())
    }

    private fun validateCircularExtends(metadata: DependangerMetadata): Diagnostics {
        val bundleIndex = metadata.bundles.associateBy { it.alias }
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()
        val cycles = mutableListOf<String>()

        fun dfs(alias: String) {
            if (alias in inStack) {
                cycles.add(alias); return
            }
            if (alias in visited) return
            visited.add(alias)
            inStack.add(alias)
            bundleIndex[alias]?.extends?.forEach { dfs(it) }
            inStack.remove(alias)
        }

        bundleIndex.keys.forEach { dfs(it) }

        val messages = cycles.map { alias ->
            DiagnosticMessage(
                code = "VALIDATION_CIRCULAR_EXTENDS",
                message = "Bundle '$alias' has circular extends dependency",
                severity = Severity.ERROR,
                processorId = "validation",
                context = emptyMap(),
            )
        }
        return Diagnostics(
            errors = messages,
            warnings = emptyList(),
            infos = emptyList(),
        )
    }

    private fun validateDeprecatedReferences(
        metadata: EffectiveMetadata,
        action: ValidationAction,
    ): Diagnostics {
        if (action == ValidationAction.IGNORE) return Diagnostics(
            errors = emptyList(),
            warnings = emptyList(),
            infos = emptyList(),
        )

        val issues = mutableListOf<DiagnosticMessage>()
        for ((alias, lib) in metadata.libraries) {
            if (lib.isDeprecated && lib.deprecation?.replacedBy != null) {
                if (lib.deprecation.replacedBy !in metadata.libraries) {
                    issues.add(
                        DiagnosticMessage(
                            code = "VALIDATION_DEPRECATED_REF",
                            message = "Library '$alias': replacedBy '${lib.deprecation.replacedBy}' does not exist",
                            severity = action.toSeverity(),
                            processorId = "validation",
                            context = emptyMap(),
                        )
                    )
                }
            }
        }
        return Diagnostics.of(*issues.toTypedArray())
    }

    private fun validateCoordinates(metadata: EffectiveMetadata): Diagnostics {
        val issues = mutableListOf<DiagnosticMessage>()
        for ((alias, lib) in metadata.libraries) {
            if (lib.group.isBlank() || lib.artifact.isBlank()) {
                issues.add(
                    DiagnosticMessage(
                        code = "VALIDATION_INVALID_COORDINATES",
                        message = "Library '$alias': empty group or artifact (group='${lib.group}', artifact='${lib.artifact}')",
                        severity = Severity.ERROR,
                        processorId = "validation",
                        context = emptyMap(),
                    )
                )
            }
            if (":" in lib.group || ":" in lib.artifact) {
                issues.add(
                    DiagnosticMessage(
                        code = "VALIDATION_INVALID_COORDINATES",
                        message = "Library '$alias': group or artifact contains ':' (group='${lib.group}', artifact='${lib.artifact}')",
                        severity = Severity.ERROR,
                        processorId = "validation",
                        context = emptyMap(),
                    )
                )
            }
        }
        return Diagnostics.of(*issues.toTypedArray())
    }

    private fun ValidationAction.toSeverity(): Severity = when (this) {
        ValidationAction.FAIL   -> Severity.ERROR
        ValidationAction.WARN   -> Severity.WARNING
        ValidationAction.INFO   -> Severity.INFO
        ValidationAction.IGNORE -> Severity.INFO
    }
}
