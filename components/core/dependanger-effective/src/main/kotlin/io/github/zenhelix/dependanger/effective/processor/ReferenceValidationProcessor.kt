package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.ValidationAction
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveVersion
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

internal class ReferenceValidationProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.VALIDATION_REFERENCES
    override val phase: ProcessingPhase = ProcessingPhase.VALIDATION
    override val constraints: Set<OrderConstraint> = setOf(
        OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER),
        OrderConstraint.runsAfter(ProcessorIds.VALIDATION_DUPLICATES),
    )
    override val isOptional: Boolean = false
    override val description: String = "Validates version references, bundle references, and deprecated references"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val validationSettings = context.settings.validation
        val diagnostics = Diagnostics.builder(metadata.diagnostics)

        diagnostics.add(
            validateUnresolvedVersions(metadata, validationSettings.onUnresolvedVersion)
        )
        diagnostics.add(validateBundleReferences(metadata))
        diagnostics.add(
            validateDeprecatedReferences(metadata, validationSettings.onDeprecatedLibrary)
        )

        return metadata.copy(diagnostics = diagnostics.build())
    }

    private fun validateUnresolvedVersions(
        metadata: EffectiveMetadata,
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

    private fun ValidationAction.toSeverity(): Severity = when (this) {
        ValidationAction.FAIL   -> Severity.ERROR
        ValidationAction.WARN   -> Severity.WARNING
        ValidationAction.INFO   -> Severity.INFO
        ValidationAction.IGNORE -> error("IGNORE must be handled before toSeverity()")
    }
}
