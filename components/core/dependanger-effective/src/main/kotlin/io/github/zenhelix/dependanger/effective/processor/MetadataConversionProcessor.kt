package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Bundle
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveBundle
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectivePlugin
import io.github.zenhelix.dependanger.effective.model.EffectiveVersion
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class MetadataConversionProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.METADATA_CONVERSION
    override val phase: ProcessingPhase = ProcessingPhase.METADATA_CONVERSION
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.PROFILE))
    override val isOptional: Boolean = false
    override val description: String = "Converts raw metadata to effective model"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val original = context.originalMetadata

        val versions = original.versions.associate { version ->
            version.name to ResolvedVersion(
                alias = version.name,
                value = version.value,
                source = VersionSource.DECLARED,
                originalRef = null,
            )
        }

        val libraries = original.libraries.associate { lib ->
            lib.alias to EffectiveLibrary(
                alias = lib.alias,
                group = lib.group,
                artifact = lib.artifact,
                version = convertVersionReference(lib.version),
                description = lib.description,
                tags = lib.tags,
                requires = lib.requires,
                deprecation = lib.deprecation,
                license = lib.license,
                constraints = lib.constraints,
                isPlatform = lib.isPlatform,
                ignoreUpdates = lib.ignoreUpdates,
            )
        }

        val plugins = original.plugins.associate { plugin ->
            plugin.alias to EffectivePlugin(
                alias = plugin.alias,
                id = plugin.id,
                version = convertVersionReference(plugin.version),
            )
        }

        val bundleIndex = original.bundles.associateBy { it.alias }
        val bundles = original.bundles.associate { bundle ->
            val resolvedLibraries = resolveBundleExtends(bundle, bundleIndex, mutableSetOf())
            bundle.alias to EffectiveBundle(
                alias = bundle.alias,
                libraries = resolvedLibraries,
                resolvedFrom = bundle.extends,
            )
        }

        return metadata.copy(
            versions = versions,
            libraries = libraries,
            plugins = plugins,
            bundles = bundles,
        )
    }

    private fun convertVersionReference(ref: VersionReference?): EffectiveVersion = when (ref) {
        is VersionReference.Literal   -> EffectiveVersion.Resolved(
            ResolvedVersion(
                alias = "",
                value = ref.version,
                source = VersionSource.DECLARED,
                originalRef = null,
            )
        )

        is VersionReference.Reference -> EffectiveVersion.Unresolved(ref.name)
        is VersionReference.Range     -> EffectiveVersion.None
        null                          -> EffectiveVersion.None
    }

    private fun resolveBundleExtends(
        bundle: Bundle,
        index: Map<String, Bundle>,
        visited: MutableSet<String>,
    ): List<String> {
        if (bundle.alias in visited) return emptyList()
        visited.add(bundle.alias)

        val parentLibraries = bundle.extends.flatMap { parentAlias ->
            val parent = index[parentAlias]
            if (parent != null) resolveBundleExtends(parent, index, visited)
            else emptyList()
        }

        return (parentLibraries + bundle.libraries).distinct()
    }
}
