package io.github.zenhelix.dependanger.features.report

import io.github.zenhelix.dependanger.core.model.DeprecationInfo
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveVersion
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.spi.ReportFormat
import io.github.zenhelix.dependanger.effective.spi.ReportSection
import io.github.zenhelix.dependanger.effective.spi.ReportSettings

internal fun emptyMetadata(): EffectiveMetadata = EffectiveMetadata(
    schemaVersion = "1.0",
    distribution = null,
    versions = emptyMap(),
    libraries = emptyMap(),
    plugins = emptyMap(),
    bundles = emptyMap(),
    diagnostics = Diagnostics.EMPTY,
    processingInfo = null,
)

internal fun sampleLibrary(
    alias: String = "spring-core",
    group: String = "org.springframework",
    artifact: String = "spring-core",
    version: String = "6.1.0",
    tags: Set<String> = emptySet(),
    deprecation: DeprecationInfo? = null,
    isPlatform: Boolean = false,
): EffectiveLibrary = EffectiveLibrary(
    alias = alias,
    group = group,
    artifact = artifact,
    version = EffectiveVersion.Resolved(ResolvedVersion(alias = "${alias}-version", value = version, source = VersionSource.DECLARED, originalRef = null)),
    description = null,
    tags = tags,
    requires = null,
    deprecation = deprecation,
    license = null,
    constraints = emptyList(),
    isPlatform = isPlatform,
)

internal fun allSectionsSettings(format: ReportFormat): ReportSettings = ReportSettings(
    format = format,
    outputDir = "build/reports/dependanger",
    sections = ReportSection.entries,
)
