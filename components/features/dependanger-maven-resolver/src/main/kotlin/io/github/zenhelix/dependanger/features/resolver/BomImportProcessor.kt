package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.core.DependangerPaths
import io.github.zenhelix.dependanger.core.model.BomImport
import io.github.zenhelix.dependanger.core.model.CredentialsProviderKey
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveVersion
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ExecutionMode
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.effective.pipeline.resolveMavenRepositories
import io.github.zenhelix.dependanger.http.client.DefaultHttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpClientFactoryKey

public class BomImportProcessor : EffectiveMetadataProcessor {
    override val id: String = PROCESSOR_ID
    override val phase: ProcessingPhase = PHASE
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.METADATA_CONVERSION))
    override val isOptional: Boolean = true
    override val description: String = "Imports dependencies from Maven BOM files"
    override fun supports(context: ProcessingContext): Boolean = true

    public companion object {
        public const val PROCESSOR_ID: String = ProcessorIds.BOM_IMPORT
        public val PHASE: ProcessingPhase = ProcessingPhase("BOM_IMPORT", ExecutionMode.SEQUENTIAL)
    }

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata {
        val bomImports = context.originalMetadata.bomImports
        if (bomImports.isEmpty()) return metadata

        val bomCache = context[BomCacheSettingsKey] ?: BomCacheSettings.DEFAULT
        val cacheDir = bomCache.directory
            ?: DependangerPaths.resolveInUserHome(DependangerPaths.BOM_CACHE_DIR)
        val repositories = context.resolveMavenRepositories()
        val credentialsProvider = context[CredentialsProviderKey]
        val httpClientFactory = context[HttpClientFactoryKey] ?: DefaultHttpClientFactory

        BomResolutionContext(
            repositories = repositories,
            credentialsProvider = credentialsProvider,
            httpClientFactory = httpClientFactory,
            cacheDirectory = cacheDir,
            ttlHours = bomCache.ttlHours,
            ttlSnapshotHours = bomCache.ttlSnapshotHours,
        ).use { resolutionCtx ->
            val resolver = BomTreeResolver(resolutionCtx, id)
            val resolvedBoms = mutableListOf<BomContent>()
            var diagnostics = metadata.diagnostics

            for (bomImport in bomImports) {
                val (resolvedVersion, versionDiagnostics) = resolveVersionReference(bomImport, context)
                diagnostics += versionDiagnostics
                if (resolvedVersion == null) continue

                if (resolvedVersion.endsWith("-SNAPSHOT")) {
                    diagnostics += Diagnostics.warning(
                        DiagnosticCodes.Bom.SNAPSHOT_WARNING,
                        "BOM '${bomImport.alias}' uses SNAPSHOT version $resolvedVersion which may be unstable",
                        id, emptyMap()
                    )
                }

                val result = resolver.resolve(
                    group = bomImport.group,
                    artifact = bomImport.artifact,
                    version = resolvedVersion,
                    diagnostics = diagnostics,
                )
                resolvedBoms.add(result.content)
                diagnostics = result.diagnostics
            }

            val versionMap = buildBomVersionMap(resolvedBoms, bomImports)
            val (updatedLibraries, enrichDiagnostics) = enrichLibraries(metadata.libraries, versionMap, diagnostics)

            return metadata.copy(
                libraries = updatedLibraries,
                diagnostics = enrichDiagnostics,
            )
        }
    }

    private fun resolveVersionReference(
        bomImport: BomImport,
        context: ProcessingContext,
    ): Pair<String?, Diagnostics> = when (val v = bomImport.version) {
        is VersionReference.Literal   -> v.version to Diagnostics.EMPTY
        is VersionReference.Reference -> {
            val found = context.originalMetadata.versions.find { it.name == v.name }
            if (found == null) {
                null to Diagnostics.error(
                    DiagnosticCodes.Bom.UNRESOLVED_VERSION,
                    "Version reference '${v.name}' not found for BOM '${bomImport.alias}'",
                    id, emptyMap()
                )
            } else {
                found.value to Diagnostics.EMPTY
            }
        }

        is VersionReference.Range     -> {
            null to Diagnostics.warning(
                DiagnosticCodes.Bom.VERSION_RANGE,
                "Version ranges not supported for BOM imports, skipping '${bomImport.alias}'",
                id, emptyMap()
            )
        }
    }

    private fun buildBomVersionMap(
        resolvedBoms: List<BomContent>,
        bomImports: List<BomImport>,
    ): Map<String, Pair<String, String>> {
        val versionMap = mutableMapOf<String, Pair<String, String>>()
        for ((index, bom) in resolvedBoms.withIndex()) {
            val bomAlias = bomImports[index].alias
            // Within a resolved BOM: last definition wins (child overrides parent, Maven behavior)
            val bomVersions = mutableMapOf<String, Pair<String, String>>()
            for (dep in bom.dependencies) {
                bomVersions["${dep.group}:${dep.artifact}"] = dep.version to bomAlias
            }
            // Across BOMs: first BOM wins
            for ((key, value) in bomVersions) {
                versionMap.putIfAbsent(key, value)
            }
        }
        return versionMap
    }

    private fun enrichLibraries(
        libraries: Map<String, EffectiveLibrary>,
        versionMap: Map<String, Pair<String, String>>,
        diagnostics: Diagnostics,
    ): Pair<Map<String, EffectiveLibrary>, Diagnostics> {
        val currentDiagnostics = Diagnostics.builder(diagnostics)
        val updatedLibraries = libraries.mapValues { (_, lib) ->
            if (lib.version.isResolved) return@mapValues lib
            val key = "${lib.group}:${lib.artifact}"
            val (version, bomAlias) = versionMap[key] ?: return@mapValues lib
            currentDiagnostics.info(
                DiagnosticCodes.Bom.VERSION_IMPORTED,
                "Version $version imported from BOM '$bomAlias' for ${lib.group}:${lib.artifact}",
                id, emptyMap()
            )
            lib.copy(
                version = EffectiveVersion.Resolved(
                    ResolvedVersion(
                        alias = lib.alias,
                        value = version,
                        source = VersionSource.BOM_IMPORT,
                        originalRef = bomAlias,
                    )
                ),
            )
        }
        return updatedLibraries to currentDiagnostics.build()
    }
}
