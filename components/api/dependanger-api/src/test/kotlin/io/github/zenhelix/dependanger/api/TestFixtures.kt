package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ExecutionMode
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ParallelMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ParallelResult
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolation
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationsExtensionKey
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree
import io.github.zenhelix.dependanger.feature.model.transitive.TransitivesExtensionKey
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflict
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflictsExtensionKey
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import io.github.zenhelix.dependanger.feature.model.updates.UpdatesExtensionKey

private val TEST_UPDATE_PHASE = ProcessingPhase("TEST_UPDATE", ExecutionMode.PARALLEL_IO)
private val TEST_SECURITY_PHASE = ProcessingPhase("TEST_SECURITY", ExecutionMode.PARALLEL_IO)
private val TEST_LICENSE_PHASE = ProcessingPhase("TEST_LICENSE", ExecutionMode.PARALLEL_IO)
private val TEST_TRANSITIVE_PHASE = ProcessingPhase("TEST_TRANSITIVE", ExecutionMode.SEQUENTIAL)

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

internal class FakeParallelProcessor<T : Any>(
    override val id: String,
    override val phase: ProcessingPhase,
    override val constraints: Set<OrderConstraint> = emptySet(),
    private val extensionKey: ExtensionKey<T>,
    private val provider: (EffectiveMetadata) -> T,
) : ParallelMetadataProcessor {
    override val isOptional: Boolean = false
    override val description: String = "Fake parallel $id for tests"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun processParallel(metadata: EffectiveMetadata, context: ProcessingContext): ParallelResult =
        ParallelResult(
            diagnostics = Diagnostics.EMPTY,
            extensions = mapOf(extensionKey to provider(metadata)),
        )
}

private val VERSION_RESOLVER_CONSTRAINT = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER))

internal fun fakeUpdateCheck(
    provider: (EffectiveMetadata) -> List<UpdateAvailableInfo>,
): FakeParallelProcessor<List<UpdateAvailableInfo>> =
    FakeParallelProcessor(
        "fake-update-check",
        TEST_UPDATE_PHASE,
        constraints = VERSION_RESOLVER_CONSTRAINT,
        extensionKey = UpdatesExtensionKey,
        provider = provider
    )

internal fun fakeSecurityCheck(
    provider: (EffectiveMetadata) -> List<VulnerabilityInfo>,
): FakeParallelProcessor<List<VulnerabilityInfo>> =
    FakeParallelProcessor(
        "fake-security-check",
        TEST_SECURITY_PHASE,
        constraints = VERSION_RESOLVER_CONSTRAINT,
        extensionKey = VulnerabilitiesExtensionKey,
        provider = provider
    )

internal fun fakeLicenseCheck(
    provider: (EffectiveMetadata) -> List<LicenseViolation>,
): FakeParallelProcessor<List<LicenseViolation>> =
    FakeParallelProcessor(
        "fake-license-check",
        TEST_LICENSE_PHASE,
        constraints = VERSION_RESOLVER_CONSTRAINT,
        extensionKey = LicenseViolationsExtensionKey,
        provider = provider
    )

internal fun fakeTransitiveResolver(
    provider: (EffectiveMetadata) -> List<TransitiveTree>,
): FakeProcessor<List<TransitiveTree>> =
    FakeProcessor(
        "fake-transitive",
        TEST_TRANSITIVE_PHASE,
        constraints = VERSION_RESOLVER_CONSTRAINT,
        extensionKey = TransitivesExtensionKey,
        provider = provider
    )

internal fun fakeConflictDetector(
    provider: (EffectiveMetadata) -> List<VersionConflict>,
): FakeProcessor<List<VersionConflict>> =
    FakeProcessor(
        "fake-conflicts",
        TEST_TRANSITIVE_PHASE,
        constraints = VERSION_RESOLVER_CONSTRAINT,
        extensionKey = VersionConflictsExtensionKey,
        provider = provider
    )
