package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.serialization.EffectiveJsonFormat
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

internal object DependangerTaskHelper {

    internal const val METADATA_FILE: String = "metadata.json"
    internal const val EFFECTIVE_FILE: String = "effective.json"

    internal fun readEffective(outputDir: File, logger: Logger): EffectiveMetadata {
        val effectiveFile = outputDir.resolve(EFFECTIVE_FILE)
        if (!effectiveFile.exists()) {
            throw GradleException("effective.json not found in $outputDir. Ensure dependangerGenerateEffective has been executed.")
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
    ) {
        logDiagnostics(result, logger)

        if (result.diagnostics.hasErrors && failOnError) {
            throw GradleException("Dependanger processing failed with ${result.diagnostics.errors.size} error(s). Set dependanger { failOnError = false } to continue despite errors.")
        }
    }

    internal fun ensureOutputDir(extension: DependangerExtension): File {
        val dir = extension.outputDirectory.get().asFile
        dir.mkdirs()
        return dir
    }
}
