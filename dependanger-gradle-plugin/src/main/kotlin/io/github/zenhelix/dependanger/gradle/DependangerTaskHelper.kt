package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.serialization.EffectiveJsonFormat
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

internal object DependangerTaskHelper {

    internal const val METADATA_FILE: String = "metadata.json"
    internal const val EFFECTIVE_FILE: String = "effective.json"

    internal fun readEffective(effectiveFile: File, logger: Logger): EffectiveMetadata {
        if (!effectiveFile.exists()) {
            throw GradleException("${effectiveFile.name} not found at $effectiveFile. Ensure dependangerGenerateEffective has been executed.")
        }
        val format = EffectiveJsonFormat()
        val result = format.readDetailed(effectiveFile.toPath())
        result.warnings.forEach { warning ->
            logger.warn("Dependanger deserialization warning: ${warning.message}")
        }
        return result.metadata
    }

    internal fun logDiagnostics(result: DependangerResult, logger: Logger) {
        result.diagnostics.errors.forEach { logger.error("Dependanger ERROR: ${it.message}") }
        result.diagnostics.warnings.forEach { logger.warn("Dependanger WARN: ${it.message}") }
        result.diagnostics.infos.forEach { logger.info("Dependanger INFO: ${it.message}") }
    }

    internal fun handleProcessingErrors(
        result: DependangerResult,
        failOnError: Boolean,
        logger: Logger,
        errorMessage: String = "Dependanger processing failed with ${result.diagnostics.errors.size} error(s). Set dependanger { failOnError = false } to continue despite errors.",
    ) {
        logDiagnostics(result, logger)

        if (result.diagnostics.hasErrors && failOnError) {
            throw GradleException(errorMessage)
        }
    }

    internal fun handleFilteredDiagnostics(
        result: DependangerResult,
        failOnError: Boolean,
        logger: Logger,
        predicate: (DiagnosticMessage) -> Boolean,
        summaryMessage: (errors: Int, warnings: Int) -> String,
        errorMessage: (Int) -> String,
    ): Boolean {
        val filteredErrors = result.diagnostics.errors.filter(predicate)
        val filteredWarnings = result.diagnostics.warnings.filter(predicate)

        filteredErrors.forEach { logger.error("Dependanger ERROR: ${it.message}") }
        filteredWarnings.forEach { logger.warn("Dependanger WARN: ${it.message}") }
        result.diagnostics.infos.filter(predicate).forEach { logger.info("Dependanger INFO: ${it.message}") }

        if (filteredErrors.isEmpty() && filteredWarnings.isEmpty()) {
            return false
        }

        logger.lifecycle(summaryMessage(filteredErrors.size, filteredWarnings.size))

        if (filteredErrors.isNotEmpty() && failOnError) {
            throw GradleException(errorMessage(filteredErrors.size))
        }
        return true
    }

    internal fun ensureOutputDir(extension: DependangerExtension): File {
        val dir = extension.outputDirectory.get().asFile
        dir.mkdirs()
        return dir
    }
}
