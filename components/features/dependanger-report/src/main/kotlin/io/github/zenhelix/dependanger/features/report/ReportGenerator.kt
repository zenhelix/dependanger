package io.github.zenhelix.dependanger.features.report

import io.github.zenhelix.dependanger.core.model.ReportSettings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public class ReportGenerator {
    public fun generate(
        effective: EffectiveMetadata,
        settings: ReportSettings = ReportSettings(),
        originalMetadata: DependangerMetadata? = null,
    ): DependangerReport = TODO()
}
