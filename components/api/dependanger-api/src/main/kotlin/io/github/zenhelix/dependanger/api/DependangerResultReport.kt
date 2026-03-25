package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.model.ReportSettings
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.GeneratedReport
import io.github.zenhelix.dependanger.effective.spi.ReportProvider
import java.util.ServiceLoader

private val reportProvider: ReportProvider by lazy {
    ServiceLoader.load(ReportProvider::class.java).firstOrNull()
        ?: throw DependangerConfigurationException(
            "No ReportProvider found on classpath. Add dependanger-report dependency.", null
        )
}

private inline fun DependangerResult.withReport(
    block: ReportProvider.(EffectiveMetadata) -> GeneratedReport,
): GeneratedReport {
    val eff = effective
        ?: throw DependangerProcessingException("Cannot generate report: no effective metadata", null, null)
    return wrapNonDependangerException({ e ->
                                           DependangerGenerationException("Report generation failed: ${e.message}", reportProvider.providerId, e)
                                       }) {
        reportProvider.block(eff)
    }
}

public fun DependangerResult.generateReport(
    settings: ReportSettings = ReportSettings.DEFAULT,
): GeneratedReport = withReport { effective ->
    generate(effective, settings, null)
}

public fun DependangerResult.writeReportTo(
    settings: ReportSettings = ReportSettings.DEFAULT,
): GeneratedReport = withReport { effective ->
    generateToFile(effective, settings, null)
}
