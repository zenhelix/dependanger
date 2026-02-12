package io.github.zenhelix.dependanger.effective

import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.processor.BundleFilterProcessor
import io.github.zenhelix.dependanger.effective.processor.CompatRulesProcessor
import io.github.zenhelix.dependanger.effective.processor.ExtractedVersionsProcessor
import io.github.zenhelix.dependanger.effective.processor.LibraryFilterProcessor
import io.github.zenhelix.dependanger.effective.processor.MetadataConversionProcessor
import io.github.zenhelix.dependanger.effective.processor.PluginFilterProcessor
import io.github.zenhelix.dependanger.effective.processor.PluginProcessor
import io.github.zenhelix.dependanger.effective.processor.ProfileProcessor
import io.github.zenhelix.dependanger.effective.processor.UsedVersionsProcessor
import io.github.zenhelix.dependanger.effective.processor.ValidationProcessor
import io.github.zenhelix.dependanger.effective.processor.VersionFallbackProcessor
import io.github.zenhelix.dependanger.effective.processor.VersionResolverProcessor

public fun coreProcessors(): List<EffectiveMetadataProcessor> = listOf(
    ProfileProcessor(),
    MetadataConversionProcessor(),
    ExtractedVersionsProcessor(),
    LibraryFilterProcessor(),
    VersionFallbackProcessor(),
    VersionResolverProcessor(),
    BundleFilterProcessor(),
    PluginFilterProcessor(),
    PluginProcessor(),
    UsedVersionsProcessor(),
    ValidationProcessor(),
    CompatRulesProcessor(),
)
