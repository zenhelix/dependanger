package io.github.zenhelix.dependanger.effective

import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.processor.BundleFilterProcessor
import io.github.zenhelix.dependanger.effective.processor.CircularDependencyValidator
import io.github.zenhelix.dependanger.effective.processor.CompatRulesProcessor
import io.github.zenhelix.dependanger.effective.processor.DuplicateValidationProcessor
import io.github.zenhelix.dependanger.effective.processor.ExtractedVersionsProcessor
import io.github.zenhelix.dependanger.effective.processor.LibraryFilterProcessor
import io.github.zenhelix.dependanger.effective.processor.MetadataConversionProcessor
import io.github.zenhelix.dependanger.effective.processor.PluginFilterProcessor
import io.github.zenhelix.dependanger.effective.processor.ProfileProcessor
import io.github.zenhelix.dependanger.effective.processor.ReferenceValidationProcessor
import io.github.zenhelix.dependanger.effective.processor.UsedVersionsProcessor
import io.github.zenhelix.dependanger.effective.processor.VersionFallbackProcessor
import io.github.zenhelix.dependanger.effective.processor.VersionResolverProcessor

internal fun coreProcessors(): List<EffectiveMetadataProcessor> = listOf(
    ProfileProcessor(),
    MetadataConversionProcessor(),
    ExtractedVersionsProcessor(),
    LibraryFilterProcessor(),
    VersionFallbackProcessor(),
    VersionResolverProcessor(),
    BundleFilterProcessor(),
    PluginFilterProcessor(),
    UsedVersionsProcessor(),
    DuplicateValidationProcessor(),
    ReferenceValidationProcessor(),
    CircularDependencyValidator(),
    CompatRulesProcessor(),
)
