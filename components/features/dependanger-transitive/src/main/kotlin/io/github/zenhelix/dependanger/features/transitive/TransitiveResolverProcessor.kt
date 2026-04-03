package io.github.zenhelix.dependanger.features.transitive

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.CredentialsProviderKey
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.withDiagnostic
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ExecutionMode
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.effective.pipeline.resolveMavenRepositories
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettingsKey
import io.github.zenhelix.dependanger.feature.model.transitive.FlatDependenciesExtensionKey
import io.github.zenhelix.dependanger.feature.model.transitive.TransitivesExtensionKey
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflictsExtensionKey
import io.github.zenhelix.dependanger.http.client.DefaultHttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpClientFactoryKey

private val logger = KotlinLogging.logger {}

private const val DEFAULT_MAX_DEPTH = 10
private const val DEFAULT_MAX_TRANSITIVES = 10_000
private const val DEFAULT_READ_TIMEOUT_MS = 30_000L
private const val LARGE_TREE_THRESHOLD = 5_000

public class TransitiveResolverProcessor : EffectiveMetadataProcessor {
    override val id: String = PROCESSOR_ID
    override val phase: ProcessingPhase = PHASE
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER))
    override val isOptional: Boolean = true
    override val description: String = "Resolves transitive dependency tree"

    public companion object {
        public const val PROCESSOR_ID: String = FeatureProcessorIds.TRANSITIVE_RESOLVER
        public val PHASE: ProcessingPhase = ProcessingPhase("TRANSITIVE_RESOLVER", ExecutionMode.SEQUENTIAL)
    }

    override fun supports(context: ProcessingContext): Boolean =
        context[TransitiveResolutionSettingsKey]?.enabled == true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata {
        val settings = context.require(TransitiveResolutionSettingsKey)
        val repositories = context.resolveMavenRepositories(settings.repositories)
        val credentialsProvider = context[CredentialsProviderKey]
        val httpClientFactory = context[HttpClientFactoryKey] ?: DefaultHttpClientFactory
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
            repositories = repositories,
            credentialsProvider = credentialsProvider,
            httpClientFactory = httpClientFactory,
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
                    group = lib.group,
                    artifact = lib.artifact,
                    version = lib.version.valueOrNull!!,
                )
            }

            val rawTrees = builder.buildTrees(directInputs)

            var updatedMetadata = metadata

            // Post-processing diagnostics: cycles
            val cycles = TransitiveTreeBuilder.collectCycles(rawTrees)
            for (cycle in cycles) {
                updatedMetadata = updatedMetadata.withDiagnostic(
                    Diagnostics.warning(
                        DiagnosticCodes.Transitive.CYCLE_DETECTED,
                        "Cyclic dependency detected: ${cycle.group}:${cycle.artifact}:${cycle.version}",
                        id, mapOf("coordinate" to "${cycle.group}:${cycle.artifact}:${cycle.version}"),
                    )
                )
            }

            // Post-processing diagnostics: tree size
            val totalNodes = builder.totalNodes
            if (totalNodes > LARGE_TREE_THRESHOLD && !builder.isLimitExceeded) {
                updatedMetadata = updatedMetadata.withDiagnostic(
                    Diagnostics.warning(
                        DiagnosticCodes.Transitive.LARGE_TREE,
                        "Large dependency tree: $totalNodes transitive nodes (consider reviewing dependencies)",
                        id, mapOf("count" to totalNodes.toString()),
                    )
                )
            }

            if (builder.isLimitExceeded) {
                updatedMetadata = updatedMetadata.withDiagnostic(
                    Diagnostics.error(
                        DiagnosticCodes.Transitive.MAX_EXCEEDED,
                        "Transitive dependency limit exceeded: $totalNodes >= $effectiveMaxTransitives, resolution was truncated",
                        id, mapOf("count" to totalNodes.toString(), "limit" to effectiveMaxTransitives.toString()),
                    )
                )
            }

            // Detect conflicts
            val conflicts = ConflictDetector.detectConflicts(
                trees = rawTrees,
                constraints = constraints,
                strategy = settings.conflictResolution,
            )

            // Apply constraints
            val constrainedTrees = ConstraintApplier.apply(rawTrees, constraints)

            // Build flat list
            val flatList = FlatListBuilder.build(constrainedTrees, libraries)

            // Check for SNAPSHOT transitives
            val snapshotDeps = flatList.filter { !it.isDirectDependency && it.version.endsWith("-SNAPSHOT") }
            for (dep in snapshotDeps) {
                updatedMetadata = updatedMetadata.withDiagnostic(
                    Diagnostics.warning(
                        DiagnosticCodes.Transitive.SNAPSHOT,
                        "Transitive dependency on SNAPSHOT version: ${dep.group}:${dep.artifact}:${dep.version}",
                        id, mapOf("coordinate" to "${dep.group}:${dep.artifact}:${dep.version}"),
                    )
                )
            }

            // Store results
            updatedMetadata = updatedMetadata
                .withExtension(TransitivesExtensionKey, constrainedTrees)
                .withExtension(FlatDependenciesExtensionKey, flatList)
                .withExtension(VersionConflictsExtensionKey, conflicts)

            // Summary diagnostic
            val transitiveCount = flatList.count { !it.isDirectDependency }
            updatedMetadata = updatedMetadata.withDiagnostic(
                Diagnostics.info(
                    DiagnosticCodes.Transitive.RESOLVED,
                    "Resolved ${libraries.size} direct, $transitiveCount transitive dependencies, ${conflicts.size} conflicts",
                    id,
                    mapOf(
                        "direct" to libraries.size.toString(),
                        "transitive" to transitiveCount.toString(),
                        "conflicts" to conflicts.size.toString(),
                    ),
                )
            )

            // Conflict diagnostics
            for (conflict in conflicts) {
                updatedMetadata = updatedMetadata.withDiagnostic(
                    Diagnostics.warning(
                        DiagnosticCodes.Transitive.CONFLICT,
                        "Version conflict: ${conflict.group}:${conflict.artifact} versions ${conflict.requestedVersions.joinToString(", ")} -> resolved ${conflict.resolvedVersion} (${conflict.resolution})",
                        id,
                        mapOf(
                            "coordinate" to "${conflict.group}:${conflict.artifact}",
                            "versions" to conflict.requestedVersions.joinToString(","),
                            "resolved" to conflict.resolvedVersion,
                            "strategy" to conflict.resolution.name,
                        ),
                    )
                )
            }

            logger.info { "Transitive resolution complete: ${libraries.size} direct, $transitiveCount transitive, ${conflicts.size} conflicts" }

            return updatedMetadata
        }
    }
}
