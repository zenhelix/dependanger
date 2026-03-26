package io.github.zenhelix.dependanger.gradle

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.writeReportTo
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.TaskAction

public abstract class ReportTask : AbstractDependangerTask() {
    init {
        description = "Generate full dependency report"
    }

    @TaskAction
    public fun execute() {
        val metadata = extension.dsl.toMetadata()
        val outputDir = DependangerTaskHelper.ensureOutputDir(extension)
        val failOnError = extension.failOnError.get()

        runWithErrorHandling(failOnError) {
            // STRICT preset — enables ALL feature processors for comprehensive report
            val dependanger = Dependanger.fromMetadata(metadata)
                .preset(ProcessingPreset.STRICT)
                .build()

            val result = runBlocking { dependanger.process() }

            if (result.effective == null) {
                DependangerTaskHelper.logDiagnostics(result, logger)
                logger.error("Dependanger: Cannot generate report -- processing failed")
                return@runWithErrorHandling
            }

            // TODO: Report generation depends on dependanger-report module being on classpath.
            // If ReportProvider SPI is not found, writeReportTo will throw DependangerConfigurationException.
            val reportSettings = metadata.settings.report
            result.writeReportTo(reportSettings)

            logger.lifecycle("Dependanger: Report generated -> $outputDir")
        }
    }
}
