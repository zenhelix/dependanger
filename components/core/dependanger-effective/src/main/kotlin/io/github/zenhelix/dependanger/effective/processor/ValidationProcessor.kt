package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.ValidationAction
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveVersion
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class ValidationProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.VALIDATION
    override val phase: ProcessingPhase = ProcessingPhase.VALIDATION
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER))
    override val isOptional: Boolean = false
    override val description: String = "Validates metadata consistency and correctness"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val validationSettings = context.settings.validation
        val diagnostics = Diagnostics.builder(metadata.diagnostics)

        diagnostics.add(validateDuplicateAliases(metadata))

        diagnostics.add(validateUnresolvedVersions(
            metadata, context, validationSettings.onUnresolvedVersion,
        ))

        diagnostics.add(validateBundleReferences(metadata))

        diagnostics.add(validateCircularExtends(context.originalMetadata))

        diagnostics.add(validateDeprecatedReferences(
            metadata, validationSettings.onDeprecatedLibrary,
        ))

        diagnostics.add(validateCoordinates(metadata))

        return metadata.copy(diagnostics = diagnostics.build())
    }

    private fun validateDuplicateAliases(metadata: EffectiveMetadata): Diagnostics {
        val allAliases = buildList {
            metadata.libraries.keys.forEach { add(it to "library") }
            metadata.plugins.keys.forEach { add(it to "plugin") }
            metadata.bundles.keys.forEach { add(it to "bundle") }
        }.groupBy({ it.first }, { it.second })

        val messages = allAliases
            .filter { it.value.size > 1 }
            .map { (alias, types) ->
                DiagnosticMessage(
                    code = DiagnosticCodes.Validation.DUPLICATE_ALIAS,
                    message = "Alias '$alias' used in multiple namespaces: ${types.joinToString()}",
                    severity = Severity.ERROR,
                    processorId = id,
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
        @Suppress("UNUSED_PARAMETER") context: ProcessingContext,
        action: ValidationAction,
    ): Diagnostics {
        if (action == ValidationAction.IGNORE) return Diagnostics.EMPTY

        val issues = metadata.libraries.mapNotNull { (alias, lib) ->
            val version = lib.version
            if (version is EffectiveVersion.Unresolved) {
                DiagnosticMessage(
                    code = DiagnosticCodes.Validation.UNRESOLVED_REF,
                    message = "Library '$alias': version reference '${version.refName}' is not resolved",
                    severity = action.toSeverity(),
                    processorId = id,
                    context = emptyMap(),
                )
            } else {
                null
            }
        }
        return Diagnostics.of(*issues.toTypedArray())
    }

    private fun validateBundleReferences(metadata: EffectiveMetadata): Diagnostics {
        val issues = metadata.bundles.flatMap { (bundleAlias, bundle) ->
            bundle.libraries.mapNotNull { libAlias ->
                if (libAlias !in metadata.libraries) {
                    DiagnosticMessage(
                        code = DiagnosticCodes.Validation.BUNDLE_REF_MISSING,
                        message = "Bundle '$bundleAlias': library '$libAlias' does not exist",
                        severity = Severity.ERROR,
                        processorId = id,
                        context = emptyMap(),
                    )
                } else {
                    null
                }
            }
        }
        return Diagnostics.of(*issues.toTypedArray())
    }

    private fun validateCircularExtends(metadata: DependangerMetadata): Diagnostics {
        val bundleIndex = metadata.bundles.associateBy { it.alias }
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun dfs(alias: String): List<String> {
            if (alias in inStack) return listOf(alias)
            if (alias in visited) return emptyList()
            visited.add(alias)
            inStack.add(alias)
            val result = bundleIndex[alias]?.extends?.flatMap { dfs(it) } ?: emptyList()
            inStack.remove(alias)
            return result
        }

        val messages = bundleIndex.keys.flatMap { dfs(it) }.map { alias ->
            DiagnosticMessage(
                code = DiagnosticCodes.Validation.CIRCULAR_EXTENDS,
                message = "Bundle '$alias' has circular extends dependency",
                severity = Severity.ERROR,
                processorId = id,
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
        if (action == ValidationAction.IGNORE) return Diagnostics.EMPTY

        val issues = metadata.libraries.mapNotNull { (alias, lib) ->
            val replacedBy = lib.deprecation?.replacedBy
            if (lib.isDeprecated && replacedBy != null && replacedBy !in metadata.libraries) {
                DiagnosticMessage(
                    code = DiagnosticCodes.Validation.DEPRECATED_REF,
                    message = "Library '$alias': replacedBy '$replacedBy' does not exist",
                    severity = action.toSeverity(),
                    processorId = id,
                    context = emptyMap(),
                )
            } else {
                null
            }
        }
        return Diagnostics.of(*issues.toTypedArray())
    }

    private fun validateCoordinates(metadata: EffectiveMetadata): Diagnostics {
        val issues = metadata.libraries.flatMap { (alias, lib) ->
            buildList {
                if (lib.group.isBlank() || lib.artifact.isBlank()) {
                    add(
                        DiagnosticMessage(
                            code = DiagnosticCodes.Validation.INVALID_COORDINATES,
                            message = "Library '$alias': empty group or artifact (group='${lib.group}', artifact='${lib.artifact}')",
                            severity = Severity.ERROR,
                            processorId = id,
                            context = emptyMap(),
                        )
                    )
                }
                if (":" in lib.group || ":" in lib.artifact) {
                    add(
                        DiagnosticMessage(
                            code = DiagnosticCodes.Validation.INVALID_COORDINATES,
                            message = "Library '$alias': group or artifact contains ':' (group='${lib.group}', artifact='${lib.artifact}')",
                            severity = Severity.ERROR,
                            processorId = id,
                            context = emptyMap(),
                        )
                    )
                }
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
