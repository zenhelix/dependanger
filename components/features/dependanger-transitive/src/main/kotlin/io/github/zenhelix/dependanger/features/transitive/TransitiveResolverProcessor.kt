package io.github.zenhelix.dependanger.features.transitive

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.DiagnosticsBuilder
import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.withDiagnostic
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.ExecutionMode
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettings
import io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettingsKey
import io.github.zenhelix.dependanger.feature.model.transitive.FlatDependenciesExtensionKey
import io.github.zenhelix.dependanger.feature.model.transitive.FlatDependency
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree
import io.github.zenhelix.dependanger.feature.model.transitive.TransitivesExtensionKey
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflict
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflictsExtensionKey
import io.github.zenhelix.dependanger.feature.support.AbstractSequentialNetworkProcessor
import io.github.zenhelix.dependanger.feature.support.NetworkProcessorInfrastructure

private val logger = KotlinLogging.logger {}

private const val DEFAULT_MAX_DEPTH = 10
private const val DEFAULT_MAX_TRANSITIVES = 10_000
private const val DEFAULT_READ_TIMEOUT_MS = 30_000L
private const val LARGE_TREE_THRESHOLD = 5_000

public class TransitiveResolverProcessor : AbstractSequentialNetworkProcessor<TransitiveResolutionSettings>() {
    override val id: String = PROCESSOR_ID
    override val phase: ProcessingPhase = PHASE
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER))
    override val isOptional: Boolean = true
    override val description: String = "Resolves transitive dependency tree"

    override val settingsKey: ProcessingContextKey<TransitiveResolutionSettings> = TransitiveResolutionSettingsKey

    override fun featureRepositories(settings: TransitiveResolutionSettings): List<Repository> = settings.repositories

    public companion object {
        public const val PROCESSOR_ID: String = FeatureProcessorIds.TRANSITIVE_RESOLVER
        public val PHASE: ProcessingPhase = ProcessingPhase("TRANSITIVE_RESOLVER", ExecutionMode.SEQUENTIAL)
    }

    override fun supports(context: ProcessingContext): Boolean =
        context[TransitiveResolutionSettingsKey]?.enabled == true

    override suspend fun executeWithInfrastructure(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        settings: TransitiveResolutionSettings,
        infrastructure: NetworkProcessorInfrastructure,
    ): EffectiveMetadata {
        val constraints = context.originalMetadata.constraints
        val effectiveMaxDepth = settings.maxDepth ?: DEFAULT_MAX_DEPTH
        val effectiveMaxTransitives = settings.maxTransitives ?: DEFAULT_MAX_TRANSITIVES

        val libraries = metadata.libraries.values
            .filter { it.version.isResolved }

        if (libraries.isEmpty()) {
            return metadata
                .withDiagnostic(
                    Diagnostics.info(
                        DiagnosticCodes.Transitive.NO_LIBS,
                        "No libraries to resolve transitives for",
                        id, emptyMap(),
                    )
                )
                .withExtension(TransitivesExtensionKey, emptyList())
                .withExtension(FlatDependenciesExtensionKey, emptyList())
                .withExtension(VersionConflictsExtensionKey, emptyList())
        }

        TransitiveResolverContext(
            repositories = infrastructure.repositories,
            credentialsProvider = infrastructure.credentialsProvider,
            httpClientFactory = infrastructure.httpClientFactory,
            cacheDirectory = settings.cacheDirectory,
            cacheTtlHours = settings.cacheTtlHours,
            readTimeoutMs = DEFAULT_READ_TIMEOUT_MS,
        ).use { ctx ->
            val builder = TransitiveTreeBuilder(
                ctx = ctx,
                maxDepth = effectiveMaxDepth,
                maxTransitives = effectiveMaxTransitives,
                scopes = settings.scopes,
                includeOptional = settings.includeOptional,
            )

            val directInputs = libraries.map { lib ->
                DirectDependencyInput(
                    coordinate = lib.coordinate,
                    version = lib.version.requireValue(),
                )
            }

            val rawTrees = builder.buildTrees(directInputs)

            val perLibraryConstrained = rawTrees.zip(libraries).map { (tree, lib) ->
                ConstraintApplier.applyToChildren(tree, lib.constraints)
            }

            val conflicts = ConflictDetector.detectConflicts(
                trees = perLibraryConstrained,
                constraints = constraints,
                strategy = settings.conflictResolution,
            )

            val constrainedTrees = ConstraintApplier.apply(perLibraryConstrained, constraints)
            val flatList = FlatListBuilder.build(constrainedTrees, libraries)

            val diagnostics = Diagnostics.builder(metadata.diagnostics)
            emitTreeDiagnostics(diagnostics, perLibraryConstrained, builder.totalNodes, builder.isLimitExceeded, effectiveMaxTransitives)
            emitSnapshotDiagnostics(diagnostics, flatList)
            emitConflictDiagnostics(diagnostics, conflicts)
            emitSummaryDiagnostic(diagnostics, libraries.size, flatList, conflicts)

            return metadata
                .copy(diagnostics = diagnostics.build())
                .withExtension(TransitivesExtensionKey, constrainedTrees)
                .withExtension(FlatDependenciesExtensionKey, flatList)
                .withExtension(VersionConflictsExtensionKey, conflicts)
        }
    }

    private fun emitTreeDiagnostics(
        diagnostics: DiagnosticsBuilder,
        trees: List<TransitiveTree>,
        totalNodes: Int,
        isLimitExceeded: Boolean,
        maxTransitives: Int,
    ) {
        val cycles = TransitiveTreeBuilder.collectCycles(trees)
        for (cycle in cycles) {
            diagnostics.warning(
                DiagnosticCodes.Transitive.CYCLE_DETECTED,
                "Cyclic dependency detected: ${cycle.coordinate}:${cycle.version}",
                id, mapOf("coordinate" to "${cycle.coordinate}:${cycle.version}"),
            )
        }

        if (totalNodes > LARGE_TREE_THRESHOLD && !isLimitExceeded) {
            diagnostics.warning(
                DiagnosticCodes.Transitive.LARGE_TREE,
                "Large dependency tree: $totalNodes transitive nodes (consider reviewing dependencies)",
                id, mapOf("count" to totalNodes.toString()),
            )
        }

        if (isLimitExceeded) {
            diagnostics.error(
                DiagnosticCodes.Transitive.MAX_EXCEEDED,
                "Transitive dependency limit exceeded: $totalNodes >= $maxTransitives, resolution was truncated",
                id, mapOf("count" to totalNodes.toString(), "limit" to maxTransitives.toString()),
            )
        }
    }

    private fun emitSnapshotDiagnostics(diagnostics: DiagnosticsBuilder, flatList: List<FlatDependency>) {
        for (dep in flatList) {
            if (!dep.isDirectDependency && dep.version.endsWith("-SNAPSHOT")) {
                diagnostics.warning(
                    DiagnosticCodes.Transitive.SNAPSHOT,
                    "Transitive dependency on SNAPSHOT version: ${dep.coordinate}:${dep.version}",
                    id, mapOf("coordinate" to "${dep.coordinate}:${dep.version}"),
                )
            }
        }
    }

    private fun emitConflictDiagnostics(diagnostics: DiagnosticsBuilder, conflicts: List<VersionConflict>) {
        for (conflict in conflicts) {
            diagnostics.warning(
                DiagnosticCodes.Transitive.CONFLICT,
                "Version conflict: ${conflict.coordinate} versions ${conflict.requestedVersions.joinToString(", ")} -> resolved ${conflict.resolvedVersion} (${conflict.resolution})",
                id,
                mapOf(
                    "coordinate" to conflict.coordinate.toString(),
                    "versions" to conflict.requestedVersions.joinToString(","),
                    "resolved" to conflict.resolvedVersion,
                    "strategy" to conflict.resolution.name,
                ),
            )
        }
    }

    private fun emitSummaryDiagnostic(
        diagnostics: DiagnosticsBuilder,
        directCount: Int,
        flatList: List<FlatDependency>,
        conflicts: List<VersionConflict>,
    ) {
        val transitiveCount = flatList.count { !it.isDirectDependency }
        diagnostics.info(
            DiagnosticCodes.Transitive.RESOLVED,
            "Resolved $directCount direct, $transitiveCount transitive dependencies, ${conflicts.size} conflicts",
            id,
            mapOf(
                "direct" to directCount.toString(),
                "transitive" to transitiveCount.toString(),
                "conflicts" to conflicts.size.toString(),
            ),
        )
        logger.info { "Transitive resolution complete: $directCount direct, $transitiveCount transitive, ${conflicts.size} conflicts" }
    }
}
