package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.features.license.model.LicenseViolation
import io.github.zenhelix.dependanger.features.license.model.LicenseViolationsExtensionKey
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.transitive.model.TransitiveTree
import io.github.zenhelix.dependanger.features.transitive.model.TransitivesExtensionKey
import io.github.zenhelix.dependanger.features.transitive.model.VersionConflict
import io.github.zenhelix.dependanger.features.transitive.model.VersionConflictsExtensionKey
import io.github.zenhelix.dependanger.features.updates.model.UpdateAvailableInfo
import io.github.zenhelix.dependanger.features.updates.model.UpdatesExtensionKey

internal class FakeProcessor<T : Any>(
    override val id: String,
    override val phase: ProcessingPhase,
    override val constraints: Set<OrderConstraint> = emptySet(),
    private val extensionKey: ExtensionKey<T>,
    private val provider: (EffectiveMetadata) -> T,
) : EffectiveMetadataProcessor {
    override val isOptional: Boolean = false
    override val description: String = "Fake $id for tests"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata =
        metadata.withExtension(extensionKey, provider(metadata))
}

internal fun fakeUpdateCheck(
    provider: (EffectiveMetadata) -> List<UpdateAvailableInfo>,
): FakeProcessor<List<UpdateAvailableInfo>> =
    FakeProcessor("fake-update-check", ProcessingPhase.UPDATE_CHECK, constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)), extensionKey = UpdatesExtensionKey, provider = provider)

internal fun fakeSecurityCheck(
    provider: (EffectiveMetadata) -> List<VulnerabilityInfo>,
): FakeProcessor<List<VulnerabilityInfo>> =
    FakeProcessor("fake-security-check", ProcessingPhase.SECURITY_CHECK, constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)), extensionKey = VulnerabilitiesExtensionKey, provider = provider)

internal fun fakeLicenseCheck(
    provider: (EffectiveMetadata) -> List<LicenseViolation>,
): FakeProcessor<List<LicenseViolation>> =
    FakeProcessor("fake-license-check", ProcessingPhase.LICENSE_CHECK, constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)), extensionKey = LicenseViolationsExtensionKey, provider = provider)

internal fun fakeTransitiveResolver(
    provider: (EffectiveMetadata) -> List<TransitiveTree>,
): FakeProcessor<List<TransitiveTree>> =
    FakeProcessor("fake-transitive", ProcessingPhase.TRANSITIVE_RESOLVER, constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)), extensionKey = TransitivesExtensionKey, provider = provider)

internal fun fakeConflictDetector(
    provider: (EffectiveMetadata) -> List<VersionConflict>,
): FakeProcessor<List<VersionConflict>> =
    FakeProcessor("fake-conflicts", ProcessingPhase.TRANSITIVE_RESOLVER, constraints = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER)), extensionKey = VersionConflictsExtensionKey, provider = provider)
