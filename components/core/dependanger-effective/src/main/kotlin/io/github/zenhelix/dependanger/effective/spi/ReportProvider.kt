package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.core.model.ReportFormat
import io.github.zenhelix.dependanger.core.model.ReportSettings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import java.nio.file.Path

/**
 * SPI for report generation.
 * Implementations are discovered via ServiceLoader by API extension functions.
 */
public interface ReportProvider {

    public val providerId: String

    public fun generate(
        effective: EffectiveMetadata,
        settings: ReportSettings,
        originalMetadata: DependangerMetadata?,
    ): GeneratedReport

    public fun generateToFile(
        effective: EffectiveMetadata,
        settings: ReportSettings,
        originalMetadata: DependangerMetadata?,
    ): GeneratedReport
}

public data class GeneratedReport(
    val format: ReportFormat,
    val content: String,
    val outputPath: Path?,
)
